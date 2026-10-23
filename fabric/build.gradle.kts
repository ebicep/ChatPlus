import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.api.attributes.Attribute

plugins {
    id("com.gradleup.shadow")
    id("multiloader-loader")
    id("net.fabricmc.fabric-loom")
    id("me.modmuss50.mod-publish-plugin")
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

loom {
    val aw = project(":common").file("src/main/resources/${rootProject.property("mod_id")}.accesswidener")
    if (aw.exists()) {
        accessWidenerPath.set(aw)
    }

    // Loom only generates IDEA run configs on the root project by default; :fabric needs them explicitly.
    runs {
        configureEach {
            generateRunConfig.set(true)
        }
    }
}

configurations {
    runtimeClasspath {
        extendsFrom(configurations.getByName("shadow"))
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    api("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")
    api("me.shedaniel.cloth:cloth-config-fabric:${rootProject.property("cloth_config_version")}") {
        exclude(group = "net.fabricmc.fabric-api")
    }

    // Fabric Kotlin
    implementation("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_kotlin_version")}")
    // Mod Menu
    implementation("com.terraformersmc:modmenu:${project.property("modmenu_version")}")

    shadow("net.java.dev.jna:jna:5.14.0")
    shadow("com.alphacephei:vosk:0.3.45")
    // MixinExtras: provided by Loom + mixinextras-common compileOnly in root project
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
                "min_minecraft_version" to rootProject.property("min_minecraft_version"),
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
    configurations = listOf(project.configurations.getByName("shadow"))
    archiveClassifier.set("dev-shadow")
    isZip64 = true
}

tasks.named("build") {
    dependsOn("shadowJar")
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

publishMods {
    println("(${project.name}) Publishing | ${rootProject.property("minecraft_version")} | ${project.name}")
    file.set(tasks.shadowJar.flatMap { it.archiveFile })
    displayName.set("${rootProject.property("mod_name")} ${project.name.uppercaseFirstChar()} v${project.version}")
    val releaseChangeLog = project.ext.get("releaseChangeLog") as? () -> String
    changelog.set(releaseChangeLog?.invoke() ?: "")
    type.set(STABLE)
    modLoaders.add(project.name)

    val mcVersions = "${rootProject.property("supported_minecraft_version")}".split(",")

    val cfToken = System.getenv("CF_TOKEN")
    if (cfToken != null) {
        println("(${project.name}) CF_TOKEN found, publishing to CurseForge")
        curseforge {
            accessToken.set(cfToken)
            projectId.set("1023333")
            minecraftVersions.addAll(mcVersions)
            client.set(true)
            requires("cloth-config", "fabric-language-kotlin", "fabric-api")
            optional("modmenu")
        }
    } else {
        println("(${project.name}) CF_TOKEN not found, not publishing to CurseForge")
    }

    val mrToken = System.getenv("MODRINTH_TOKEN")
    if (mrToken != null) {
        println("(${project.name}) MODRINTH_TOKEN found, publishing to Modrinth")
        modrinth {
            accessToken.set(mrToken)
            projectId.set("cJlZ132G")
            minecraftVersions.addAll(mcVersions)
            environment.set(CLIENT_ONLY)
            requires("9s6osm5g", "Ha28R6CL", "P7dR8mSH")
            optional("mOgUt4GM")
        }
    } else {
        println("(${project.name}) MODRINTH_TOKEN not found, not publishing to Modrinth")
    }
}

// Implement mcgradleconventions loader attribute
val loaderAttribute: Attribute<String> =
    Attribute.of("io.github.mcgradleconventions.loader", String::class.java)

listOf("apiElements", "runtimeElements").forEach { variant ->
    configurations.named(variant) {
        attributes {
            attribute(loaderAttribute, "fabric")
        }
    }
}

sourceSets.configureEach {
    val compileCp = configurations.named(compileClasspathConfigurationName)
    val runtimeCp = configurations.named(runtimeClasspathConfigurationName)

    compileCp.configure {
        attributes {
            attribute(loaderAttribute, "fabric")
        }
    }

    runtimeCp.configure {
        attributes {
            attribute(loaderAttribute, "fabric")
        }
    }
}