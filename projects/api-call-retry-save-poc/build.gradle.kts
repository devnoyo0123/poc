import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.lombok") version "1.9.22"
}

group = "com.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

extra["resilience4jVersion"] = "2.1.0"

dependencies {
    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Boot Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Spring Boot AOP (required for @Retry annotation)
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Spring Boot Actuator (for health checks)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // H2 Database
    runtimeOnly("com.h2database:h2")

    // Resilience4j Retry
    implementation("io.github.resilience4j:resilience4j-retry:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${property("resilience4jVersion")}")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Spring Boot Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("api-call-retry-save-poc.jar")
}
