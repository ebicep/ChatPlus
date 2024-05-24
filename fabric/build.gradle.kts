import org.gradle.kotlin.dsl.support.uppercaseFirstChar

plugins {
    id("com.github.johnrengelman.shadow")
}

repositories {
    maven {
        url = uri("https://maven.quiltmc.org/repository/release/")
    }
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentFabric: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentFabric.extendsFrom(common)
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-fabric:${rootProject.property("architectury_version")}")
    modApi("me.shedaniel.cloth:cloth-config-fabric:${rootProject.property("cloth_config_version")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionFabric")) { isTransitive = false }

    // Fabric Kotlin
    modImplementation("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_kotlin_version")}")
    // Mod Menu
    modImplementation("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    include("com.alphacephei:vosk:0.3.45")
}

tasks.processResources {
    inputs.property("group", rootProject.property("maven_group"))
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "group" to rootProject.property("maven_group"),
                "version" to project.version,

                "mod_id" to rootProject.property("mod_id"),
                "minecraft_version" to rootProject.property("minecraft_version"),
                "architectury_version" to rootProject.property("architectury_version"),
                "fabric_kotlin_version" to rootProject.property("fabric_kotlin_version"),
                "cloth_config_version" to rootProject.property("cloth_config_version"),

                "mod_name" to rootProject.property("mod_name"),
                "mod_description" to rootProject.property("mod_description"),
                "mod_authors" to rootProject.property("mod_authors"),
            )
        )
    }
}

tasks.shadowJar {
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
                // kotlin for fabric
                curseforge = "fabric-language-kotlin"
                modrinth = "Ha28R6CL"
            }
            depends { // Mark as a required dependency
                // fabric api
                curseforge = "fabric-api"
                modrinth = "P7dR8mSH"
            }
            optional {
                // mod menu
                curseforge = "modmenu"
                modrinth = "mOgUt4GM"
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