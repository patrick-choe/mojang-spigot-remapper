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
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Files

abstract class RemapTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    @get:Optional
    abstract val action: Property<Action>

    @get:Input
    @get:Optional
    abstract val skip: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val inputTask: Property<AbstractArchiveTask>

    @get:Input
    @get:Optional
    abstract val archiveClassifier: Property<String>

    @get:Input
    @get:Optional
    abstract val archiveName: Property<String>

    @get:InputDirectory
    @get:Optional
    abstract val archiveDirectory: DirectoryProperty

    @TaskAction
    fun execute() {
        if (skip.orNull != true) {
            val task = inputTask.orNull ?: project.tasks.named("jar").get() as AbstractArchiveTask
            val archiveFile = task.archiveFile.get().asFile

            val version =
                version.orNull ?: throw IllegalStateException("Version should be specified for ${project.path}.")

            val targetFile = File(
                archiveDirectory.orNull?.asFile ?: archiveFile.parentFile,
                archiveName.orNull ?: archiveClassifier.orNull?.let { classifier ->
                    task.fileNameWithClassifier(classifier)
                } ?: archiveFile.name
            )

            var fromFile = archiveFile
            var toFile = Files.createTempFile(null, ".jar").toFile()

            val action = action.getOrElse(Action.MOJANG_TO_SPIGOT)
            val iterator = action.procedures.iterator()
            var shouldRemove = false
            while (iterator.hasNext()) {
                val procedure = iterator.next()
                procedure.remap(project, version, fromFile, toFile)

                if (shouldRemove) {
                    fromFile.delete()

                }

                if (iterator.hasNext()) {
                    fromFile = toFile
                    toFile = Files.createTempFile(null, ".jar").toFile()
                    shouldRemove = true
                }
            }

            toFile.copyTo(targetFile, true)
            toFile.delete()
            println("Successfully obfuscate jar (${project.name}, $action)")
        }
    }

    private companion object {
        private fun AbstractArchiveTask.fileNameWithClassifier(classifier: String): String {
            return "${archiveBaseName.get()}-${archiveVersion.get()}-$classifier.jar"
        }

        private val nullOutputStream = PrintStream(object : OutputStream() {
            override fun write(b: Int) {}
        })
    }

    enum class Action(internal vararg val procedures: ActualProcedure) {
        MOJANG_TO_SPIGOT(ActualProcedure.MOJANG_OBF, ActualProcedure.OBF_SPIGOT),
        MOJANG_TO_OBF(ActualProcedure.MOJANG_OBF),
        OBF_TO_MOJANG(ActualProcedure.OBF_MOJANG),
        OBF_TO_SPIGOT(ActualProcedure.OBF_SPIGOT),
        SPIGOT_TO_MOJANG(ActualProcedure.SPIGOT_OBF, ActualProcedure.OBF_MOJANG),
        SPIGOT_TO_OBF(ActualProcedure.SPIGOT_OBF);
    }

    internal enum class ActualProcedure(
        private val mapping: (version: String) -> String,
        private val inheritance: (version: String) -> String,
        private val reversed: Boolean = false
    ) {
        MOJANG_OBF(
            { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-mojang@txt" },
            { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-mojang" },
            true
        ),
        OBF_MOJANG(
            { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-mojang@txt" },
            { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-obf" }
        ),
        SPIGOT_OBF(
            { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-spigot@csrg" },
            { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT" },
            true
        ),
        OBF_SPIGOT(
            { version -> "org.spigotmc:minecraft-server:$version-R0.1-SNAPSHOT:maps-spigot@csrg" },
            { version -> "org.spigotmc:spigot:$version-R0.1-SNAPSHOT:remapped-obf" }
        );

        fun remap(project: Project, version: String, jarFile: File, outputFile: File) {
            val dependencies = project.dependencies

            val mappingFile =
                project.configurations.detachedConfiguration(dependencies.create(mapping(version))).singleFile
            val inheritanceFiles =
                project.configurations.detachedConfiguration(dependencies.create(inheritance(version))).files.toList()

            Jar.init(jarFile).use { inputJar ->
                // ignore SS multiple main class err
                val err = System.err
                System.setErr(nullOutputStream)

                Jar.init(inheritanceFiles).use { inheritanceJar ->
                    val mapping = JarMapping()
                    mapping.loadMappings(mappingFile.canonicalPath, reversed, false, null, null)

                    val provider = JointProvider()
                    provider.add(JarProvider(inputJar))
                    provider.add(JarProvider(inheritanceJar))
                    mapping.setFallbackInheritanceProvider(provider)

                    val mapper = JarRemapper(mapping)
                    mapper.remapJar(inputJar, outputFile)
                }

                System.setErr(err)
            }
        }
    }
}
