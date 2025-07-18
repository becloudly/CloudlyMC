plugins {
    kotlin("jvm") version "2.2.0"
    id("com.gradleup.shadow") version "9.0.0-beta15"
    id("java")
    id("org.sonarqube") version "6.0.1.5171"
    id("org.owasp.dependencycheck") version "11.1.0"
    id("org.jetbrains.dokka") version "1.9.20"
    id("jacoco")
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

group = "de.cloudly"
version = "1.0.0-alpha_2"

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

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Coroutines for async operations (performance optimization)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // SQLite JDBC driver
    implementation("org.xerial:sqlite-jdbc:3.50.2.0")

    // MySQL JDBC driver
    implementation("com.mysql:mysql-connector-j:9.3.0")
    
    // JSON library for import/export functionality
    implementation("org.json:json:20250517")
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.14")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    
    // Detekt for static analysis
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
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

// SonarQube configuration
sonarqube {
    properties {
        property("sonar.projectKey", "becloudly_CloudlyMC")
        property("sonar.organization", "becloudly")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.kotlin.source.version", "1.9")
        property("sonar.sources", "app/src/main")
        property("sonar.tests", "app/src/test")
        property("sonar.kotlin.binaries", "build/classes/kotlin/main")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.exclusions", "**/BuildConstants.kt")
    }
}

// OWASP Dependency Check configuration
dependencyCheck {
    formats = listOf("HTML", "JSON", "XML")
    outputDirectory = "build/reports/dependency-check"
    suppressionFile = "owasp-suppressions.xml"
    failBuildOnCVSS = 7.0f
    nvd {
        apiKey = System.getenv("NVD_API_KEY") ?: ""
        delay = 2000
    }
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

// Test configuration
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    finalizedBy(tasks.jacocoTestReport)
}

// Dokka configuration for documentation
tasks.dokkaHtml {
    outputDirectory.set(file("build/docs/dokka"))
    
    dokkaSourceSets {
        named("main") {
            moduleName.set("CloudlyMC")
            moduleVersion.set(project.version.toString())
            
            sourceRoots.from(file("app/src/main/kotlin"))
            
            includes.from("README.md")
            
            jdkVersion.set(17)
            
            externalDocumentationLink {
                url.set(uri("https://docs.oracle.com/en/java/javase/17/docs/api/").toURL())
            }
            
            externalDocumentationLink {
                url.set(uri("https://jd.papermc.io/paper/1.20/").toURL())
            }
        }
    }
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    baseline = file("$projectDir/detekt-baseline.xml")
    
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}
