/*
 * Copyright (C) 2023 PatrickKR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.patrick.gradle.remapper

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

/**
 * Task for remapping JAR files between different mapping types (Mojang, Spigot, Obfuscated).
 *
 * This task is fully compatible with Gradle's configuration cache.
 *
 * Example usage:
 * ```kotlin
 * tasks.remap {
 *     version.set("1.20.4")
 *     inputFile.set(tasks.jar.flatMap { it.archiveFile })
 *     archiveClassifier.set("remapped")
 * }
 * ```
 */
abstract class RemapTask @Inject constructor(
    private val layout: ProjectLayout
) : DefaultTask() {

    /**
     * The Minecraft version to use for mapping resolution.
     * This is required and must be set.
     */
    @get:Input
    abstract val version: Property<String>

    /**
     * The remapping action to perform.
     * Defaults to [Action.MOJANG_TO_SPIGOT].
     */
    @get:Input
    @get:Optional
    abstract val action: Property<Action>

    /**
     * Whether to skip this task.
     * Defaults to false.
     */
    @get:Input
    @get:Optional
    abstract val skip: Property<Boolean>

    /**
     * The input JAR file to remap.
     * This should be set to the archive file of a JAR task.
     */
    @get:InputFile
    abstract val inputFile: RegularFileProperty

    /**
     * Optional classifier to append to the output file name.
     * If set, the output will be named `{baseName}-{version}-{classifier}.jar`.
     */
    @get:Input
    @get:Optional
    abstract val archiveClassifier: Property<String>

    /**
     * Optional explicit name for the output archive.
     * Takes precedence over [archiveClassifier].
     */
    @get:Input
    @get:Optional
    abstract val archiveName: Property<String>

    /**
     * Optional directory for the output archive.
     * Defaults to the build/libs directory.
     */
    @get:Optional
    @get:OutputDirectory
    abstract val archiveDirectory: DirectoryProperty

    /**
     * Collection of mapping files to use for remapping.
     * These are resolved based on the [version] and [action] at configuration time.
     */
    @get:InputFiles
    abstract val mappingFiles: ConfigurableFileCollection

    /**
     * Collection of inheritance provider files (JAR files) to use for remapping.
     * These are resolved based on the [version] and [action] at configuration time.
     */
    @get:InputFiles
    abstract val inheritanceFiles: ConfigurableFileCollection

    /**
     * The output file. This is automatically computed based on input file,
     * classifier, and output directory settings.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Project name for logging purposes.
     * Set automatically by the plugin.
     */
    @get:Internal
    abstract val projectName: Property<String>

    init {
        group = "build"
        description = "Remaps JAR from one mapping type to another"

        // Set up conventional output file location
        outputFile.convention(
            inputFile.flatMap { input ->
                val baseName = input.asFile.nameWithoutExtension
                val extension = input.asFile.extension.ifEmpty { "jar" }

                val fileName = archiveName.orElse(
                    archiveClassifier.map { classifier ->
                        "$baseName-$classifier.$extension"
                    }.orElse(input.asFile.name)
                )

                archiveDirectory.map { dir ->
                    dir.file(fileName.get())
                }.orElse(
                    layout.buildDirectory.file("libs/${fileName.get()}")
                )
            }
        )
    }

    @TaskAction
    fun execute() {
        if (skip.getOrElse(false)) {
            logger.lifecycle("Skipping remap task for ${projectName.getOrElse("unknown")}")
            return
        }

        val inputJarFile = inputFile.get().asFile
        val targetFile = outputFile.get().asFile

        if (!inputJarFile.exists()) {
            throw IllegalStateException("Input file does not exist: $inputJarFile")
        }

        val remapAction = action.getOrElse(Action.MOJANG_TO_SPIGOT)
        val procedures = remapAction.procedures
        val mappingFilesList = mappingFiles.files.toList()
        val inheritanceFilesList = inheritanceFiles.files.toList()

        if (mappingFilesList.size != procedures.size) {
            throw IllegalStateException(
                "Expected ${procedures.size} mapping files for action $remapAction, but got ${mappingFilesList.size}. " +
                        "Make sure mappingFiles is configured correctly."
            )
        }

        if (inheritanceFilesList.size != procedures.size) {
            throw IllegalStateException(
                "Expected ${procedures.size} inheritance files for action $remapAction, but got ${inheritanceFilesList.size}. " +
                        "Make sure inheritanceFiles is configured correctly."
            )
        }

        var fromFile = inputJarFile
        var toFile = Files.createTempFile("remap", ".jar").toFile()
        var shouldDeleteFrom = false

        try {
            for (i in procedures.indices) {
                val procedure = procedures[i]
                val mappingFile = mappingFilesList[i]
                val inheritanceFile = inheritanceFilesList[i]

                remap(procedure, mappingFile, inheritanceFile, fromFile, toFile)

                if (shouldDeleteFrom) {
                    fromFile.delete()
                }

                if (i < procedures.size - 1) {
                    fromFile = toFile
                    toFile = Files.createTempFile("remap", ".jar").toFile()
                    shouldDeleteFrom = true
                }
            }

            targetFile.parentFile?.mkdirs()
            toFile.copyTo(targetFile, overwrite = true)
            logger.lifecycle("Successfully remapped JAR (${projectName.getOrElse("unknown")}, $remapAction) -> $targetFile")
        } finally {
            // Clean up temp file
            if (toFile.exists() && toFile != targetFile) {
                toFile.delete()
            }
        }
    }

    private fun remap(
        procedure: Procedure,
        mappingFile: File,
        inheritanceFile: File,
        jarFile: File,
        outputFile: File
    ) {
        Jar.init(jarFile).use { inputJar ->
            Jar.init(inheritanceFile).use { inheritanceJar ->
                val mapping = JarMapping()
                mapping.loadMappings(mappingFile.canonicalPath, procedure.reversed, false, null, null)

                val provider = JointProvider()
                provider.add(JarProvider(inputJar))
                provider.add(JarProvider(inheritanceJar))
                mapping.setFallbackInheritanceProvider(provider)

                val mapper = JarRemapper(mapping)
                mapper.remapJar(inputJar, outputFile)
            }
        }
    }

    /**
     * Available remapping actions.
     */
    enum class Action(internal vararg val procedures: Procedure) {
        /** Remap from Mojang mappings to Spigot mappings (most common use case) */
        MOJANG_TO_SPIGOT(Procedure.MOJANG_OBF, Procedure.OBF_SPIGOT),

        /** Remap from Mojang mappings to obfuscated */
        MOJANG_TO_OBF(Procedure.MOJANG_OBF),

        /** Remap from obfuscated to Mojang mappings */
        OBF_TO_MOJANG(Procedure.OBF_MOJANG),

        /** Remap from obfuscated to Spigot mappings */
        OBF_TO_SPIGOT(Procedure.OBF_SPIGOT),

        /** Remap from Spigot mappings to Mojang mappings */
        SPIGOT_TO_MOJANG(Procedure.SPIGOT_OBF, Procedure.OBF_MOJANG),

        /** Remap from Spigot mappings to obfuscated */
        SPIGOT_TO_OBF(Procedure.SPIGOT_OBF);
    }

    /**
     * Individual remapping procedures with their mapping coordinates.
     */
    enum class Procedure(
        /** Maven coordinate function for the mapping file */
        val mappingCoordinate: (version: String) -> String,
        /** Maven coordinate function for the inheritance provider JAR */
        val inheritanceCoordinate: (version: String) -> String,
        /** Whether to reverse the mapping direction */
        val reversed: Boolean = false
    ) {
        MOJANG_OBF(
            mappingCoordinate = { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-mojang@txt" },
            inheritanceCoordinate = { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-mojang" },
            reversed = true
        ),
        OBF_MOJANG(
            mappingCoordinate = { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-mojang@txt" },
            inheritanceCoordinate = { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-obf" }
        ),
        SPIGOT_OBF(
            mappingCoordinate = { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-spigot@csrg" },
            inheritanceCoordinate = { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT" },
            reversed = true
        ),
        OBF_SPIGOT(
            mappingCoordinate = { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-spigot@csrg" },
            inheritanceCoordinate = { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-obf" }
        );
    }

}

/**
 * Kept for backwards compatibility.
 * @see RemapTask.Procedure
 */
@Deprecated("Use RemapTask.Procedure instead", ReplaceWith("RemapTask.Procedure"))
typealias ActualProcedure = RemapTask.Procedure
