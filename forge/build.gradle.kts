import org.gradle.kotlin.dsl.support.uppercaseFirstChar

plugins {
    id("com.github.johnrengelman.shadow")
}

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    forge.apply {
        convertAccessWideners.set(true)
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)

        mixinConfig("chatplus-common.mixins.json")
        mixinConfig("chatplus.mixins.json")
    }
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentForge: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentForge.extendsFrom(common)
}

repositories {
    // KFF
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

dependencies {
    forge("net.minecraftforge:forge:${rootProject.property("forge_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-forge:${rootProject.property("architectury_version")}")
    modApi("me.shedaniel.cloth:cloth-config-forge:${rootProject.property("cloth_config_version")}")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionForge")) { isTransitive = false }

    // Kotlin For Forge
    implementation("thedarkcolour:kotlinforforge:${rootProject.property("kotlin_for_forge_version")}")

    forgeRuntimeLibrary("net.java.dev.jna:jna:5.14.0")
    forgeRuntimeLibrary("com.alphacephei:vosk:0.3.45")

    include("com.alphacephei:vosk:0.3.45")

    // mixin extras
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.6")
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.6")
    implementation("io.github.llamalad7:mixinextras-common:0.3.6")
    include("io.github.llamalad7:mixinextras-forge:0.3.6")
}

tasks.processResources {
    inputs.property("group", rootProject.property("maven_group"))
    inputs.property("version", project.version)

    filesMatching("META-INF/mods.toml") {
        expand(mapOf(
            "group" to rootProject.property("maven_group"),
            "version" to project.version,

            "mod_id" to rootProject.property("mod_id"),
            "minecraft_version" to rootProject.property("minecraft_version"),
            "architectury_version" to rootProject.property("architectury_version"),
            "kotlin_for_forge_version" to rootProject.property("kotlin_for_forge_version"),
            "cloth_config_version" to rootProject.property("cloth_config_version"),

            "mod_name" to rootProject.property("mod_name"),
            "mod_description" to rootProject.property("mod_description"),
            "mod_authors" to rootProject.property("mod_authors"),
        ))
    }
}

tasks.shadowJar {
    exclude("fabric.mod.json")
    exclude("architectury.common.json")
    configurations = listOf(shadowCommon)
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    injectAccessWidener.set(true)
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set(null as String?)
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.sourcesJar {
    val commonSources = project(":common").tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}

components.getByName("java") {
    this as AdhocComponentWithVariants
    this.withVariantsFromConfiguration(project.configurations["shadowRuntimeElements"]) {
        skip()
    }
}

unifiedPublishing {
    project {
        println("(${project.name}) Publishing | ${rootProject.property("minecraft_version")} | ${project.name}")
        displayName.set("${rootProject.property("mod_name")} ${project.name.uppercaseFirstChar()} v${project.version}")
        changelog.set((project.ext.get("releaseChangeLog") as () -> String)())
        gameVersions.set(listOf("${rootProject.property("minecraft_version")}"))
        gameLoaders.set(listOf(project.name))
        releaseType.set("release")

        mainPublication.set(tasks.remapJar.get().archiveFile) // Declares the publicated jar

        relations {
            depends { // Mark as a required dependency
                // architectury
                curseforge = "architectury-api"
                modrinth = "lhGA9TYQ"
            }
            depends { // Mark as a required dependency
                // cloth config
                curseforge = "cloth-config"
                modrinth = "9s6osm5g"
            }
            depends { // Mark as a required dependency
                // kotlin for forge
                curseforge = "kotlin-for-forge"
                modrinth = "ordsPcFz"
            }
        }

        val cfToken = System.getenv("CF_TOKEN")
        if (cfToken != null) {
            println("(${project.name}) CF_TOKEN found, publishing to CurseForge")
            curseforge {
                token = cfToken
                id = "1023333" // Required, must be a string, ID of CurseForge project
            }
        } else {
            println("(${project.name}) CF_TOKEN not found, not publishing to CurseForge")
        }

        val mrToken = System.getenv("MODRINTH_TOKEN")
        if (mrToken != null) {
            println("(${project.name}) MODRINTH_TOKEN found, publishing to Modrinth")
            modrinth {
                token = mrToken
                id = "cJlZ132G" // Required, must be a string, ID of Modrinth project
            }
        } else {
            println("(${project.name}) CF_TOKEN not found, not publishing to CurseForge")
        }
    }
}