import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    id("nu.studer.jooq") version "8.2.1"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

// 소스 디렉토리 설정
sourceSets.main {
    java.srcDirs("src/main/kotlin", "src/generated/kotlin")
}

// jOOQ 코드 생성 설정
jooq {
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(true)

            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN

                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5435/webflux_lab"
                    user = "postgres"
                    password = "postgres"
                }

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"  // public 스키마만 생성
                    }

                    target.apply {
                        packageName = "com.example.webfluxsselab.jooq"
                        directory = "src/generated/kotlin"
                    }
                }
            }
        }
    }
}

extra["springReactorVersion"] = "2023.0.0"

dependencies {
    // Spring Boot WebFlux
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")

    // R2DBC (Database - Optional)
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

    // jOOQ
    implementation("org.jooq:jooq-kotlin:3.19.5")
    implementation("org.jooq:jooq:3.19.5") // jOOQ R2DBC를 위해 필요
    jooqGenerator("org.postgresql:postgresql:42.7.1")

    // Devtools
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

// 컴파일 전에 jOOQ 코드 생성 실행
tasks.compileKotlin {
    dependsOn("generateJooq")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
