plugins {
    val jvmVersion = libs.versions.fabric.kotlin.get()
        .split("+kotlin.")[1]
        .split("+")[0]

    kotlin("jvm").version(jvmVersion)
    kotlin("plugin.serialization").version(jvmVersion)
    alias(libs.plugins.fabric.loom)
    `maven-publish`
    java
}

allprojects {
    group = "net.casual"
    version = "1.1.10"

    apply(plugin = "fabric-loom")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    repositories {
        mavenLocal()
        maven("https://maven.supersanta.me/snapshots")
        maven("https://maven.maxhenkel.de/repository/public")
        maven("https://maven.parchmentmc.org/")
        maven("https://jitpack.io")
        maven("https://maven.nucleoid.xyz")
        maven("https://api.modrinth.com/maven")
        maven("https://maven.andante.dev/releases/")
        mavenCentral()
    }

    configurations.all {
        // This is to resolve any conflicts with arcade-datagen
        resolutionStrategy {
            force(rootProject.libs.arcade)
        }
    }

    dependencies {
        val libs = rootProject.libs

        minecraft(libs.minecraft)
        @Suppress("UnstableApiUsage")
        mappings(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip")
        })
        modImplementation(libs.fabric.loader)
        modImplementation(libs.fabric.api)
        modImplementation(libs.fabric.kotlin)

        modImplementation(libs.arcade)

        modImplementation(libs.map.canvas)
    }

    java {
        withSourcesJar()
    }

    loom {
        runs {
            create("datagenClient") {
                client()
                programArgs("--arcade-datagen")
                runDir = "run-datagen"
            }
        }
    }

    tasks {
        processResources {
            inputs.property("version", version)
            filesMatching("fabric.mod.json") {
                expand(mutableMapOf("version" to version))
            }
        }

        jar {
            from("LICENSE")
        }
    }
}

subprojects {
    if (this.path != ":minigames-common") {
        dependencies {
            api(project(path = ":minigames-common", configuration = "namedElements"))
        }
    }
}

dependencies {
    include(libs.arcade)
    include(libs.map.canvas)
    include(modImplementation(libs.server.replay.get())!!)

    for (subproject in project.subprojects) {
        implementation(project(path = subproject.path, configuration = "namedElements"))
        include(subproject)
    }

    includeImplementation(libs.casual.database)
}

private fun DependencyHandler.includeModImplementation(dependencyNotation: Any) {
    include(dependencyNotation)
    modImplementation(dependencyNotation)
}

private fun DependencyHandler.includeImplementation(dependencyNotation: Any) {
    include(dependencyNotation)
    implementation(dependencyNotation)
}