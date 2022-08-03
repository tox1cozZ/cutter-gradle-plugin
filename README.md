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
    compileOnly 'com.github.tox1cozZ.cutter-gradle-plugin:tools:master-SNAPSHOT'
}

cutter {
    packages.include("your/package/for/processing/**")
    targets {
        minecraftForgeSideOnlyLegacy() // Forge 1.7.10 and older
        minecraftForgeSideOnly() // Forge 1.8-1.13
        minecraftForgeOnlyIn() // Forge 1.14+
        minecraftFabricEnvironment() // Fabric
    }
}

afterEvaluate {
    // Replace string constants in classes
    tasks.withType(CutterTask) {
        replaceTokens {
            token '${VERSION}', project.version
        }
    }

    serverJar {
        excludeMinecraftAssets() // Exclude assets directory (keep lang files)
    }
}
```
