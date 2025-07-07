plugins {
    // Apply the kotlin plugin version for all projects
    kotlin("jvm") version "2.2.0" apply false
    kotlin("kapt") version "2.2.0" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("eclipse")
    id("xyz.jpenilla.run-velocity") version "2.3.1" apply false
}

allprojects {
    group = "de.cloudly"
    version = "1.0.0"
}

// Add clean task to the root project
tasks.register("clean") {
    delete(rootProject.buildDir)
}

// Add task to build all projects
tasks.register("build") {
    dependsOn(":app:build")
}
