/*
 * Copyright (C) 2022 PatrickKR
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
    abstract val inputTask: Property<AbstractArchiveTask>

    @get:Input
    @get:Optional
    abstract val archiveClassifier: Property<String>

    @get:Input
    @get:Optional
    abstract val archiveName: Property<String>

    @TaskAction
    fun execute() {
        if (skip.orNull != true) {
            val task = inputTask.orNull ?: project.tasks.named("jar").get() as AbstractArchiveTask
            val archiveFile = task.archiveFile.get().asFile

            val obfOutput = File(archiveFile.parentFile, "remapped-obf.jar")
            val spigotOutput = File(archiveFile.parentFile, "remapped-spigot.jar")

            val targetFile = archiveName.orNull?.let { name ->
                File(archiveFile.parent, name)
            } ?: archiveClassifier.orNull?.let { classifier ->
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
                throw IllegalStateException("Mojang and Spigot mapping should be specified for ${project.path}.")
            }
        }
    }

    private companion object {
        private fun AbstractArchiveTask.fileNameWithClassifier(classifier: String): String {
            return "${archiveBaseName.get()}-${archiveVersion.get()}-$classifier.jar"
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