plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.10.2"
}

group = "com"
repositories { mavenCentral() }

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    runtimeOnly("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testImplementation("com.tngtech.archunit:archunit:1.5.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration")
    }
    finalizedBy(tasks.jacocoTestReport)
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests that require external infrastructure."
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    shouldRunAfter(tasks.test)
}

jacoco { toolVersion = "0.8.15" }

val logicClasses = files(layout.buildDirectory.dir("classes/java/main"))
    .asFileTree
    .matching {
        include("com/book/core/**/application/usecase/**/*.class")
        include("com/book/core/**/domain/**/*.class")
        exclude("com/book/core/**/domain/Q*.class")
    }

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(logicClasses)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(logicClasses)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.jacocoTestCoverageVerification)
    dependsOn(integrationTest)
    dependsOn("verifyJavaLineLength")
}
tasks.jar { enabled = false }

val javaSourceFiles = fileTree("src") {
    include("main/java/**/*.java", "test/java/**/*.java")
}

tasks.register("verifyJavaLineLength") {
    group = "verification"
    description = "Checks that Java source lines do not exceed 140 characters."
    inputs.files(javaSourceFiles)

    doLast {
        val violations = javaSourceFiles.files.flatMap { sourceFile ->
            sourceFile.readLines().mapIndexedNotNull { index, line ->
                val length = line.codePointCount(0, line.length)
                if (length > 140) {
                    "${sourceFile.relativeTo(projectDir)}:${index + 1}: $length characters"
                } else {
                    null
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Java source lines must not exceed 140 characters:\n${violations.joinToString("\n")}",
            )
        }
    }
}

spotless {
    ratchetFrom("origin/main")
    java {
        eclipse("4.41").configFile(".config/spotless/google-derived-java-style.xml")
        removeUnusedImports()
        forbidWildcardImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
