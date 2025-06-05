/*
 * Cloudly - High-performance Minecraft Plugin
 * Supports Minecraft 1.18+ on PaperMC servers
 * Uses Epoch Semantic Versioning (Epoch.Major.Minor.Patch-TAG)
 */

plugins {
    kotlin("jvm") version "2.1.21"
    id("com.gradleup.shadow") version "9.0.0-beta15"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    // PaperMC API for Minecraft 1.18+
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    
    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Coroutines for async operations (performance optimization)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // SQLite JDBC driver
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")

    // MySQL JDBC driver
    implementation("com.mysql:mysql-connector-j:8.4.0")
    
    // JSON library for import/export functionality
    implementation("org.json:json:20240303")
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

// Version using Epoch Semantic Versioning
val epochVersion = "1.0.0.0"
version = epochVersion

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
    
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("Cloudly-${version}.jar")

        // Relocate dependencies to avoid conflicts
        relocate("kotlin", "cloudly.libs.kotlin")
        relocate("kotlinx", "cloudly.libs.kotlinx")

        // Merge service files to avoid duplicate META-INF/services/java.sql.Driver
        mergeServiceFiles()

        minimize()
    }
      build {
        dependsOn(shadowJar)
    }
    
    processResources {
        // Set duplicates strategy to handle duplicate files
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        
        val props = mapOf(
            "version" to version,
            "name" to "Cloudly",
            "main" to "cloudly.CloudlyPlugin"
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
