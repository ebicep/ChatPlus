plugins {
    id("com.github.johnrengelman.shadow")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentNeoForge: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentNeoForge.extendsFrom(common)
}

repositories {
    // KFF
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
    maven {
        setUrl("https://maven.neoforged.net/releases/")
    }
}

dependencies {
    neoForge("net.neoforged:neoforge:${rootProject.property("neoforge_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-neoforge:${rootProject.property("architectury_version")}")
    modApi("me.shedaniel.cloth:cloth-config-neoforge:${rootProject.property("cloth_config_version")}")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionForge")) { isTransitive = false }

    // Kotlin For Forge
    implementation("thedarkcolour:kotlinforforge:${rootProject.property("kotlin_for_forge_version")}")
}

tasks.processResources {
    inputs.property("group", rootProject.property("maven_group"))
    inputs.property("version", project.version)

    filesMatching("META-INF/mods.toml") {
        expand(
            mapOf(
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
            )
        )
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
    atAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
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