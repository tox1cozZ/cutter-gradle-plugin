plugins {
    `maven-publish`
}

dependencies {
    compileOnly("org.jetbrains", "annotations", "23.0.0")
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}