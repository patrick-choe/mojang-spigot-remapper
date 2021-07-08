package com.github.patrick.gradle.remapper

import org.gradle.api.Plugin
import org.gradle.api.Project

class MojangSpigotRemapperPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configurations.apply {
            create("mojangMapping")
            create("spigotMapping")
        }

        target.tasks.register("remap", RemapTask::class.java)
    }
}