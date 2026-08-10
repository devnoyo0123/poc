plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "1.9.23"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    val kotestVersion = "5.9.1"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
    testImplementation("org.testcontainers:postgresql:1.20.6")
    testImplementation("org.testcontainers:junit-jupiter:1.20.6")

    testRuntimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.20.6")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    val orbStackSocket = file("${System.getProperty("user.home")}/.orbstack/run/docker.sock")
    if (orbStackSocket.exists()) {
        environment("DOCKER_HOST", "unix://${orbStackSocket.absolutePath}")
        environment("DOCKER_API_VERSION", "1.43")
        environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
        systemProperty("docker.host", "unix://${orbStackSocket.absolutePath}")
        systemProperty("docker.api.version", "1.43")
    }
}

kotlin {
    jvmToolchain(21)
}
