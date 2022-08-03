plugins {
    kotlin("jvm") version "1.7.10" apply false
    java
    idea
}

allprojects {
    group = "io.github.tox1cozz.cutter"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply<JavaPlugin>()
    apply<IdeaPlugin>()

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        withSourcesJar()
    }

    idea {
        module.isDownloadSources = true
    }
}