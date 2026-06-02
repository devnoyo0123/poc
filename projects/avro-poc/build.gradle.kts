import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://repo.spring.io/milestone")
}

extra["confluentVersion"] = "7.5.0"
extra["awsGlueVersion"] = "1.1.0"
extra["springCloudVersion"] = "2023.0.0"

dependencies {
    configurations {
        all {
            exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
            exclude(group = "org.slf4j", module = "slf4j-reload4j")
            exclude(group = "ch.qos.logback", module = "logback-classic")
        }
    }

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")

    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")

    // Spring Kafka
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Kotlin Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:3.6.0")

    // Avro
    implementation("org.apache.avro:avro:1.11.3")

    // Confluent Schema Registry
    implementation("io.confluent:kafka-avro-serializer:${property("confluentVersion")}")
    implementation("io.confluent:kafka-schema-registry:${property("confluentVersion")}")

    // AWS Glue Schema Registry (TODO: Verify Maven coordinates)
    // implementation("software.amazon.glue.schema-registry:schema-registry-library:${property("awsGlueVersion")}")
    // implementation("software.amazon.glue.schema-registry:schema-registry-serde:${property("awsGlueVersion")}")

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:kafka:1.19.3")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${extra["springCloudVersion"]}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
}
