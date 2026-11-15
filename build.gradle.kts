import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile


buildscript {
    repositories { mavenCentral() }

    dependencies {
        classpath(kotlin("gradle-plugin", version = "2.3.+"))
        classpath(kotlin("serialization", version = "2.3.+"))
    }
}

plugins {
    java
    kotlin("jvm") version "2.3.+"
    kotlin("plugin.serialization") version "2.3.+" apply false
    id("com.gradleup.shadow") version "9.4.2" apply false
    id("net.fabricmc.fabric-loom") version(providers.gradleProperty("fabric_loom_version")) apply false
    id("net.neoforged.moddev") version(providers.gradleProperty("moddevgradle_version")) apply false
    id("multiloader-common") apply false
    id("multiloader-loader") apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.0" apply false
    id("dev.isxander.mtk.accessx") version "0.1.1" apply false
}

subprojects {
    extensions.findByType(LoomGradleExtensionAPI::class.java)?.let { loom ->
        loom.log4jConfigs.from(file("log4j-dev.xml"))
    }

    project.ext.set("releaseChangeLog", {
        val changelogFile = file("../docs/changelogs/${rootProject.version}.md")
        if (changelogFile.exists()) {
            changelogFile.readText().trim()
        } else {
            ""
        }
    })
}

allprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    val archivesNameProp = rootProject.findProperty("archives_name")?.toString()
        ?: rootProject.property("archives_base_name").toString()
    base.archivesName.set("${archivesNameProp}-${project.name}")
    version = rootProject.property("mod_version").toString()
    group = rootProject.property("maven_group").toString()

    repositories {
        // Add repositories to retrieve artifacts from in here.
        // You should only use this when depending on other mods because
        // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
        // See https://docs.gradle.org/current/userguide/declaring_repositories.html
        // for more information about repositories.
        exclusiveContent {
            forRepository {
                maven {
                    name = "KotlinForForge"
                    url = uri("https://thedarkcolour.github.io/KotlinForForge/")
                }
            }
            filter {
                includeGroup("thedarkcolour")
            }
        }
        maven { url = uri("https://maven.shedaniel.me/") }
        maven { url = uri("https://maven.terraformersmc.com") }
        mavenCentral()
    }

    dependencies {
        compileOnly(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        implementation("net.java.dev.jna:jna:5.14.0")
        implementation("com.alphacephei:vosk:0.3.45")
        compileOnly("io.github.llamalad7:mixinextras-common:0.5.4")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

    tasks.withType<Test>().configureEach {
        enabled = false
    }

    tasks.register("checkMetadataDeps") {
        doLast {
            configurations.compileClasspath.get().resolvedConfiguration.firstLevelModuleDependencies
                .filter { it.moduleGroup == "org.jetbrains.kotlinx" && it.moduleName == "kotlinx-metadata-jvm" }
                .forEach {
                    println("Using kotlinx-metadata-jvm version: ${it.moduleVersion}")
                }
        }
    }


    java {
        withSourcesJar()
    }
}

tasks.register<Jar>("mergeLoaderJars") {
    group = "build"
    description =
        "Dev-only: merges Fabric and NeoForge shadow JARs. Not for release (conflicting loader metadata). " +
        "Use :fabric:shadowJar and :neoforge:shadowJar for publishing."

    val fabricJar = project(":fabric").tasks.named<Jar>("shadowJar")
    val neoforgeJar = project(":neoforge").tasks.named<Jar>("shadowJar")

    dependsOn(fabricJar)
    dependsOn(neoforgeJar)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val archivesNameProp = rootProject.findProperty("archives_name")?.toString()
        ?: rootProject.property("archives_base_name").toString()
    archiveFileName.set("${archivesNameProp}-multiloader-${rootProject.version}.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    from({ zipTree(fabricJar.get().archiveFile) })
    from({ zipTree(neoforgeJar.get().archiveFile) })
}
