# Mojang -> Spigot Remapper

---

[![](https://img.shields.io/gradle-plugin-portal/v/io.github.patrick.remapper?style=for-the-badge)](https://plugins.gradle.org/plugin/io.github.patrick.remapper)

### Description

Gradle plugin for remapping mojang-mapped artifact to spigot-mapped

Uses [SpecialSource](https://github.com/md-5/SpecialSource) for remapping.

---

### Applying Plugin

#### Kotlin

Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

```kotlin
plugins {
    id("io.github.patrick.remapper") version "1.1.0"
}
```

Using [legacy plugin application](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application):
<details><summary>Click to View</summary>

```kotlin
buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("io.github.patrick-choe:mojang-spigot-remapper:1.1.0")
    }
}

apply(plugin = "io.github.patrick.remapper")
```
</details>

#### Groovy

Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

```groovy
plugins {
    id "io.github.patrick.remapper" version "1.1.0"
}
```

Using [legacy plugin application](https://docs.gradle.org/current/userguide/plugins.html#sec:old_plugin_application):
<details><summary>Click to View</summary>

```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath "io.github.patrick-choe:mojang-spigot-remapper:1.1.0"
    }
}

apply plugin: "io.github.patrick.remapper"
```
</details>

---

### How to use

- Setup by applying the plugin to the project. See above for more details.
   - This will add `spigotMapping` and `mojangMapping` configurations along with `remap` task to the project.
- Configure `spigotMapping` and `mojangMapping` in dependency handler scope.
   - Example:
      ```kotlin
      dependencies {
          "..."
          mojangMapping("org.spigotmc:minecraft-server:1.17-R0.1-SNAPSHOT:maps-mojang@txt")
          spigotMapping("org.spigotmc:minecraft-server:1.17-R0.1-SNAPSHOT:maps-spigot@csrg")
      }
      ```
- Run `remap` task to remap mojang-mapped jar artifact to spigot-mapped.
   - Advanced configuration option is available below.

---

### Advanced configuration option

```kotlin
tasks {
    remap {
        // If this option is set to true, the entire task would be skipped.
        skip.set(true)

        // Use this option to explicitly set the input task.
        // Defaults to `jar` task.
        inputTask.set(jar)

        // If this option is used, instead of overwriting an existing artifact,
        // the remap output would be available at file named as `archiveName`.
        // Note that `archiveName` has higher priority over `archiveClassifier`. 
        archiveName.set("${project.name}-${project.version}-remapped.jar")

        // If this option is used, instead of overwriting an existing artifact,
        // the remap output would be available at "${archiveBaseName}-${archiveVersion}-${archiveClassifier}.jar"
        // Note that `archiveName` has higher priority over `archiveClassifier`. 
        archiveClassifier.set("remapped")
    }
}
```

---

### Additional information

All contributions are welcome!
If you encounter any problems, or have suggestions, please leave an issue!

Contact me at

- Discord: PatrickKR [#] 0645
- Email: mailpatrickkr [@] gmail.com

---