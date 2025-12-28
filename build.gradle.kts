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

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.gradle.plugin-publish") version "1.3.0"
    signing
}

kotlin {
    jvmToolchain(21)
}

group = "io.github.patrick-choe"
version = "1.5.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.md-5:SpecialSource:1.11.4")

    compileOnly(gradleApi())
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    compileKotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
}

gradlePlugin {
    website.set("https://github.com/patrick-choe/${rootProject.name}")
    vcsUrl.set("https://github.com/patrick-choe/${rootProject.name}.git")
    plugins {
        create("mojangSpigotRemapper") {
            id = "io.github.patrick.remapper"
            displayName = "Mojang - Spigot Remapper"
            group = rootProject.group
            implementationClass = "io.github.patrick.gradle.remapper.MojangSpigotRemapperPlugin"
            description = "Gradle plugin for remapping mojang-mapped artifact to spigot-mapped"
            tags.set(listOf("kotlin", "special source", "remapper", "mojang", "spigot", "minecraft"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mojangSpigotRemapper") {
            from(components["java"])

            repositories {
                mavenLocal()

                maven {
                    name = "central"

                    credentials {
                        username = findProperty("nexusUsername") as String? ?: ""
                        password = findProperty("nexusPassword") as String? ?: ""
                    }

                    url = uri(
                        if (version.toString().endsWith("SNAPSHOT")) {
                            "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                        } else {
                            "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                        }
                    )
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
    isRequired = findProperty("nexusUsername") != null
}
