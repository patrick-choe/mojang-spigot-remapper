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

import groovy.lang.MissingPropertyException
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.6.10"
    id("com.gradle.plugin-publish") version "0.20.0"
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "io.github.patrick-choe"
version = "1.2.0"

repositories {
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib"))

    implementation("net.md-5:SpecialSource:1.11.0")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    withType<DokkaTask> {
        dokkaSourceSets {
            named("main") {
                displayName.set(rootProject.name)
                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(uri("https://github.com/patrick-choe/${rootProject.name}/tree/main/src/main/kotlin").toURL())
                    remoteLineSuffix.set("#L")
                }
            }
        }
    }

    create<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    create<Jar>("dokkaJar") {
        archiveClassifier.set("javadoc")
        dependsOn("dokkaHtml")

        from("$buildDir/dokka/html/") {
            include("**")
        }
    }
}

gradlePlugin {
    plugins {
        create("mojangSpigotRemapper") {
            id = "io.github.patrick.remapper"
            displayName = "Mojang - Spigot Remapper"
            group = rootProject.group
            implementationClass = "io.github.patrick.gradle.remapper.MojangSpigotRemapperPlugin"
            description = "Gradle plugin for remapping mojang-mapped artifact to spigot-mapped"
        }
    }
}

pluginBundle {
    description = "Gradle plugin for remapping mojang-mapped artifact to spigot-mapped"
    website = "https://github.com/patrick-choe/${rootProject.name}"
    vcsUrl = "https://github.com/patrick-choe/${rootProject.name}.git"
    tags = listOf("kotlin", "special source", "remapper", "mojang", "spigot", "minecraft")
}

try {
    publishing {
        publications {
            create<MavenPublication>("mojangSpigotRemapper") {
                from(components["java"])
                artifact(tasks["sourcesJar"])
                artifact(tasks["dokkaJar"])

                repositories {
                    mavenLocal()

                    maven {
                        name = "central"

                        credentials {
                            val nexusUsername: String by project
                            val nexusPassword: String by project
                            username = nexusUsername
                            password = nexusPassword
                        }

                        url = uri(if (version.endsWith("SNAPSHOT")) {
                            "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                        } else {
                            "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                        })
                    }
                }

                pom {
                    name.set(rootProject.name)
                    description.set("Gradle plugin for remapping mojang-mapped artifact to spigot-mapped")
                    url.set("https://github.com/patrick-choe/${rootProject.name}")

                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("patrick-choe")
                            name.set("PatrickKR")
                            email.set("mailpatrickkr@gmail.com")
                            url.set("https://github.com/patrick-choe")
                            roles.addAll("developer")
                            timezone.set("Asia/Seoul")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/patrick-choe/${rootProject.name}.git")
                        developerConnection.set("scm:git:ssh://github.com:patrick-choe/${rootProject.name}.git")
                        url.set("https://github.com/patrick-choe/${rootProject.name}")
                    }
                }
            }
        }
    }

    signing {
        isRequired = true
        sign(tasks["sourcesJar"], tasks["dokkaJar"], tasks["jar"])
        sign(publishing.publications["mojangSpigotRemapper"])
    }
} catch (ignored: MissingPropertyException) {}