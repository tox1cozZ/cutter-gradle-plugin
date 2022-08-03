import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.0.0"
}

val shade: Configuration by configurations.creating
configurations.compileOnly.get().extendsFrom(shade)

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    shade("org.ow2.asm", "asm-tree", "9.3")
    api(project(":tools"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks.named<Jar>("jar") {
    from(shade.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/INDEX.LIST")
        exclude("**/module-info.class")
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