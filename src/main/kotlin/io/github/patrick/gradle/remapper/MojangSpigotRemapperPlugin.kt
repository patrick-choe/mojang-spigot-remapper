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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask

/**
 * Gradle plugin for remapping JAR files between Mojang and Spigot mappings.
 *
 * This plugin registers a `remap` task that can be configured to remap JARs
 * using Spigot's mapping files.
 *
 * Example usage in build.gradle.kts:
 * ```kotlin
 * plugins {
 *     id("io.github.patrick.remapper") version "1.5.0"
 * }
 *
 * tasks.remap {
 *     version.set("1.20.4")
 *     inputFile.set(tasks.jar.flatMap { it.archiveFile })
 *     archiveClassifier.set("remapped")
 * }
 * ```
 *
 * The plugin automatically resolves mapping files from Spigot's Maven repository.
 * Make sure to add the Spigot repository to your build:
 * ```kotlin
 * repositories {
 *     maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
 * }
 * ```
 */
class MojangSpigotRemapperPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Add Spigot repository for mapping resolution
        target.repositories.maven { repo ->
            repo.name = "spigot-snapshots"
            repo.setUrl("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        }

        target.tasks.register("remap", RemapTask::class.java) { task ->
            task.projectName.set(target.name)

            // Default to using the jar task's output if available
            target.tasks.named("jar", AbstractArchiveTask::class.java).configure { jarTask ->
                task.inputFile.convention(jarTask.archiveFile)
            }

            // Configure mapping and inheritance files lazily based on version and action
            task.mappingFiles.from(
                target.provider {
                    val version = task.version.orNull
                        ?: throw IllegalStateException("Version must be set for remap task in project ${target.path}")
                    val action = task.action.getOrElse(RemapTask.Action.MOJANG_TO_SPIGOT)

                    action.procedures.map { procedure ->
                        target.configurations.detachedConfiguration(
                            target.dependencies.create(procedure.mappingCoordinate(version))
                        ).apply {
                            isTransitive = false
                        }.singleFile
                    }
                }
            )

            task.inheritanceFiles.from(
                target.provider {
                    val version = task.version.orNull
                        ?: throw IllegalStateException("Version must be set for remap task in project ${target.path}")
                    val action = task.action.getOrElse(RemapTask.Action.MOJANG_TO_SPIGOT)

                    action.procedures.map { procedure ->
                        target.configurations.detachedConfiguration(
                            target.dependencies.create(procedure.inheritanceCoordinate(version))
                        ).apply {
                            isTransitive = false
                        }.singleFile
                    }
                }
            )
        }
    }
}
