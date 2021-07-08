package com.github.patrick.gradle.remapper

import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.io.File

abstract class RemapTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val skip: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val classifierName: Property<String>

    @get:Input
    @get:Optional
    abstract val inputTask: Property<AbstractArchiveTask>

    @TaskAction
    fun execute() {
        if (skip.orNull != true) {
            val task = inputTask.orNull ?: project.tasks.named("jar").get() as AbstractArchiveTask
            val archiveFile = task.archiveFile.get().asFile

            val obfOutput = File(archiveFile.parentFile, "remapped-obf.jar")
            val spigotOutput = File(archiveFile.parentFile, "remapped-spigot.jar")

            val targetFile = classifierName.orNull?.let { classifier ->
                File(archiveFile.parent, task.fileNameWithClassifier(classifier))
            } ?: archiveFile

            val configurations = project.configurations
            val mojangMapping = configurations.named("mojangMapping").get().firstOrNull()
            val spigotMapping = configurations.named("spigotMapping").get().firstOrNull()

            if (mojangMapping != null && spigotMapping != null) {
                remap(archiveFile, obfOutput, mojangMapping, true)
                remap(obfOutput, spigotOutput, spigotMapping)

                spigotOutput.copyTo(targetFile, true)
                obfOutput.delete()
                spigotOutput.delete()
                println("Successfully obfuscate jar (${project.name})")
            } else {
                project.logger.warn("Mojang and Spigot mapping should be specified for ${
                    task.path.dropLast(1).takeLastWhile { it != ':' }
                }.")
            }
        }
    }

    private companion object {
        private fun AbstractArchiveTask.fileNameWithClassifier(classifier: String): String {
            return "${archiveBaseName.get()}-${archiveVersion}-$classifier.jar"
        }

        private fun remap(jarFile: File, outputFile: File, mappingFile: File, reversed: Boolean = false) {
            val inputJar = Jar.init(jarFile)

            val mapping = JarMapping()
            mapping.loadMappings(mappingFile.canonicalPath, reversed, false, null, null)

            val provider = JointProvider()
            provider.add(JarProvider(inputJar))
            mapping.setFallbackInheritanceProvider(provider)

            val mapper = JarRemapper(mapping)
            mapper.remapJar(inputJar, outputFile)
            inputJar.close()
        }
    }
}