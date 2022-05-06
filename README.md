# Mojang -> Spigot Remapper

---

[![](https://img.shields.io/gradle-plugin-portal/v/io.github.patrick.remapper?style=for-the-badge)](https://plugins.gradle.org/plugin/io.github.patrick.remapper)

### Description

Gradle plugin for remapping mojang-mapped artifact to spigot-mapped

Uses [SpecialSource](https://github.com/md-5/SpecialSource) for remapping.

---

### Note

For those seeking for nms & stable remapper, try using `userdev` from [PaperMC/paperweight](https://github.com/PaperMC/paperweight) project.

I will not (can not) provide any additional help for additional non-spigot features (incl. support for specific spigot fork)

There could be some methods that do not remap properly, so it is required to find some workarounds for those methods.

---

### Applying plugin

#### Kotlin

Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

```kotlin
plugins {
    id("io.github.patrick.remapper") version "1.3.0"
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
        classpath("io.github.patrick-choe:mojang-spigot-remapper:1.3.0")
    }
}

apply(plugin = "io.github.patrick.remapper")
```
</details>

#### Groovy

Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

```groovy
plugins {
    id "io.github.patrick.remapper" version "1.3.0"
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
        classpath "io.github.patrick-choe:mojang-spigot-remapper:1.3.0"
    }
}

apply plugin: "io.github.patrick.remapper"
```
</details>

---

### How to use

- Prerequisite: Run BuildTools with `--remapped` option.
- Setup by applying the plugin to the project. See above for more details.
   - This will add `remap` task to the project.
- Configure `remap` task. Advanced configuration option is available in the below section.
   - Example:
      ```kotlin
      tasks {
          remap {
              version.set("1.18.2")
          }
      }
      ```
- Run `remap` task to remap mojang-mapped jar artifact to spigot-mapped.

---

### Advanced configuration option

```kotlin
tasks {
    remap {
        // Required
        // Specify minecraft (spigot) version of your project.
        // TODO: Auto-detect library version by default
        version.set("1.18.2")

        // Use this option to change remapping action.
        // Defaults to `RemapTask.Action.MOJANG_TO_SPIGOT`.
        action.set(RemapTask.Action.MOJANG_TO_SPIGOT)

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

        // Use this option to set output directory of remapped archive file.
        // Defaults to output directory of input task.
        archiveDirectory.set(File(projectDir, "output"))
    }
}
```

---

### Additional information

All contributions are welcome!

If you encounter any problems, or have suggestions, [please leave an issue](https://github.com/patrick-choe/mojang-spigot-remapper/issues)!

Contact me at

- Discord: PatrickKR [#] 0645
- Email: mailpatrickkr [@] gmail.com

---

### Changelog

#### 1.0.0
 - Initial release

#### 1.1.0
 - Add archiveName, archiveClassifier option

#### 1.1.1
 - Update library versions
 - Add note about `userdev`

#### 1.1.2
 - Fix publication error from v1.1.1

#### 1.2.0
 - Fix internal implementation of remapping
 - Change configuration options

#### 1.3.0
 - Update library versions
 - Add archiveDirectory option

---