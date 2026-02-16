plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "9.2.2"
    id("java")
}

group = "de.cloudly"
version = "1.0.1"

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
    // Paper API for 1.21+
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Coroutines for async operations (performance optimization)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // SQLite JDBC driver
    implementation("org.xerial:sqlite-jdbc:3.51.2.0")

    // MySQL JDBC driver
    implementation("com.mysql:mysql-connector-j:9.4.0")
    
    // HikariCP for MySQL connection pooling
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // JSON library for import/export functionality
    implementation("org.json:json:20250517")
    
    // HTTP client for GitHub API communication
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
}

// Java version updated to 21 (LTS)
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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
    jvmToolchain(21)
}

tasks {
    shadowJar {
        // Relocate Kotlin stdlib to avoid conflicts
        relocate("kotlin", "de.cloudly.libs.kotlin")
        relocate("kotlinx", "de.cloudly.libs.kotlinx")
        
        // Relocate database drivers to avoid conflicts
        relocate("org.sqlite", "de.cloudly.libs.sqlite")
        relocate("com.mysql", "de.cloudly.libs.mysql")
        
        // Relocate HikariCP to avoid conflicts
        relocate("com.zaxxer.hikari", "de.cloudly.libs.hikari")
        
        // Relocate HTTP client to avoid conflicts
        relocate("okhttp3", "de.cloudly.libs.okhttp3")
        relocate("okio", "de.cloudly.libs.okio")
        
        // Relocate JSON library to avoid conflicts
        relocate("org.json", "de.cloudly.libs.json")

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