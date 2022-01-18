plugins {
    kotlin("jvm") version "1.6.10" apply false // "1.4.32"
    java
}

allprojects {
    group = "ua.tox1cozz.cutter"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        withSourcesJar()
    }
}