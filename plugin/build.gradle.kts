import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.18.0"
}

val shadow: Configuration by configurations.creating
configurations.compileOnly.get().extendsFrom(shadow)

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.4.1") { // 0.4.1 0.2.0
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }

    shadow("org.ow2.asm", "asm-tree", "9.2")

    compileOnly("org.apache.ant", "ant", "1.10.12")

    api(project(":tools"))
}

tasks.named<Jar>("jar") {
    from(shadow.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("**/module-info.class")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}

gradlePlugin {
    plugins {
        create("cutter") {
            id = "ua.tox1cozz.cutter"
            displayName = "Cutter Plugin"
            description = "A Gradle plugin for splitting code into client and server builds."
            implementationClass = "ua.tox1cozz.cutter.CutterPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/tox1cozZ/cutter-gradle-plugin"
    vcsUrl = "https://github.com/tox1cozZ/cutter-gradle-plugin.git"
    description = "A Gradle plugin for splitting code into client and server builds."
    tags = listOf("cut", "jar", "build", "client build", "server build")
}