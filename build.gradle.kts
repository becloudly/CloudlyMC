plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0-beta15"
    id("xyz.jpenilla.run-velocity") version "2.3.1"
}

group = "de.cloudly"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    // Paper API für 1.20+
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Coroutines for async operations (performance optimization)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // SQLite JDBC driver
    implementation("org.xerial:sqlite-jdbc:3.50.2.0")

    // MySQL JDBC driver
    implementation("com.mysql:mysql-connector-j:9.3.0")
    
    // JSON library for import/export functionality
    implementation("org.json:json:20250517")
}

// Java version for Minecraft 1.18+ compatibility
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)
}

tasks {
    runVelocity {
        // Configure the Velocity version for our task.
        velocityVersion("3.4.0-SNAPSHOT")
    }

    shadowJar {
        // Relocate Kotlin stdlib to avoid conflicts
        relocate("kotlin", "de.cloudly.libs.kotlin")

        // Minimize the JAR to only include used classes
        minimize()

        // Set the classifier to null so shadowJar replaces the regular jar
        archiveClassifier.set("")
        
        // Set the base name to "cloudly" instead of "app"
        archiveBaseName.set("cloudly")

        // Include both platform implementations
        from(sourceSets.main.get().output)
    }

    // Make build task depend on shadowJar instead of jar
    build {
        dependsOn(shadowJar)
    }

    // Disable the regular jar task to avoid conflicts
    jar {
        enabled = false
    }

    // Process resources to replace version in plugin.yml
    processResources {
        expand("version" to project.version)
    }
}

val templateSource = file("src/main/templates")
val templateDest = layout.buildDirectory.dir("generated/sources/templates")
val generateTemplates = tasks.register<Copy>("generateTemplates") {
    val props = mapOf("version" to project.version)
    inputs.properties(props)

    from(templateSource)
    into(templateDest)
    expand(props)
}

sourceSets.main.configure { java.srcDir(generateTemplates.map { it.outputs }) }

// VS Code friendly task to prepare templates
tasks.register("prepareVSCode") {
    dependsOn(generateTemplates)
}
