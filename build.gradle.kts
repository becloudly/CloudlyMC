plugins {
    kotlin("jvm") version "2.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

group = "de.cloudly"
version = "1.0.0-alpha_6"

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
    // Paper API for 1.20+
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    
    // Folia API for Folia support
    compileOnly("dev.folia:folia-api:1.20.1-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Coroutines for async operations (performance optimization)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // SQLite JDBC driver
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")

    // MySQL JDBC driver
    implementation("com.mysql:mysql-connector-j:9.3.0")
    
    // JSON library for import/export functionality
    implementation("org.json:json:20250517")
    
    // HTTP client for GitHub API communication
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
}

// Java version for Minecraft 1.18+ compatibility
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    
    sourceSets {
        main {
            java.srcDirs("app/src/main/java")
            kotlin.srcDirs("app/src/main/kotlin")
            resources.srcDirs("app/src/main/resources")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks {
    shadowJar {
        // Relocate Kotlin stdlib to avoid conflicts
        relocate("kotlin", "de.cloudly.libs.kotlin")
        
        // Relocate database drivers to avoid conflicts
        relocate("org.sqlite", "de.cloudly.libs.sqlite")
        relocate("com.mysql", "de.cloudly.libs.mysql")

        // Minimize the JAR to only include used classes
        minimize()

        // Merge service files to prevent duplicates
        mergeServiceFiles()

        // Set the classifier to null so shadowJar replaces the regular jar
        archiveClassifier.set("")
        
        // Set the base name to "cloudly" instead of "app"
        archiveBaseName.set("cloudly")

        // Include Paper implementation
        from(sourceSets.main.get().output)
    }

    // Make build task depend on shadowJar instead of jar
    build {
        dependsOn(shadowJar)
    }

    // Configure jar task to match local build
    jar {
        enabled = true
        archiveBaseName.set("cloudly")
        // Use the project version without additional versioning
        archiveVersion.set("${project.version}")
    }

    // Process resources to replace version in plugin.yml
    processResources {
        expand("version" to project.version)
    }
}

val templateSource = file("app/src/main/templates")
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