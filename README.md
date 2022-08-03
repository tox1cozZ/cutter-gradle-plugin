# cutter-gradle-plugin
A Gradle plugin for splitting code into client and server builds.

```
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.tox1cozZ.cutter-gradle-plugin:plugin:master-SNAPSHOT'
    }
}

apply plugin: 'io.github.tox1cozz.cutter'

repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.tox1cozZ.cutter-gradle-plugin:plugin:master-SNAPSHOT'
}

cutter {
    packages.include("your/package/for/processing/**")
    targets {
        // minecraftForgeSideOnlyLegacy() // Forge 1.7.10 and older
        // minecraftForgeSideOnly() // Forge 1.8-1.13
        // minecraftFabricEnvironment() // Fabric
        minecraftForgeOnlyIn() // Forge 1.14+
    }
}
```
