import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.ow2.asm:asm-tree:9.3")
    api(project(":tools"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    relocate("org.objectweb.asm", "io.github.tox1cozz.cutter.lib.org.objectweb.asm")
    relocate("kotlin", "io.github.tox1cozz.cutter.lib.kotlin")
    dependencies {
        exclude(dependency("org.jetbrains:annotations"))
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("pluginMaven") {
                pom {
                    name.set("cutter-gradle-plugin")
                    description.set("A Gradle plugin for splitting code into client and server builds")
                    url.set("https://github.com/tox1cozZ/cutter-gradle-plugin")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("tox1cozZ")
                            name.set("Dmytro Tukin")
                        }
                    }
                }
            }
        }
    }
}

gradlePlugin {
    plugins {
        create("cutter") {
            id = "io.github.tox1cozz.cutter"
            displayName = "Cutter Plugin"
            description = "A Gradle plugin for splitting code into client and server builds."
            implementationClass = "io.github.tox1cozz.cutter.CutterPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/tox1cozZ/cutter-gradle-plugin"
    vcsUrl = "https://github.com/tox1cozZ/cutter-gradle-plugin.git"
    description = "A Gradle plugin for splitting code into client and server builds."
    tags = listOf("cut", "jar", "build", "client build", "server build")
}