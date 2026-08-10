import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

// =============================================================================
// Version matrix (한 곳에서 통합 관리)
// 주의: plugins {} 블록은 top-level val 참조 불가 → plugin 버전은 리터럴로 기입.
//       아래 jooqVersion val 는 본문(extra override / 주석용)에서만 사용.
// =============================================================================
// Spring Boot 3.4.1 / Kotlin 2.0.21 / jOOQ 3.20.16 (3.19+ 공식 Gradle plugin)
val jooqVersion = "3.20.16"

plugins {
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    // ⭐ jOOQ 3.19+ 공식 Gradle codegen plugin (nu.studer.jooq 대체)
    id("org.jooq.jooq-codegen-gradle") version "3.20.16"
}

group = "com.career"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

// Spring Boot BOM 이 관리하는 jOOQ 버전을 우리가 사용하는 plugin 버전과 정렬.
// (Spring Boot 3.4 BOM 기본 jOOQ 는 3.19.x 계정이므로 3.20.x 로 override)
extra["jooq.version"] = jooqVersion

dependencies {
    // ----- Spring Boot -----
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ----- Kotlin Coroutines -----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // ----- R2DBC drivers / pool -----
    // io.asyncer:r2dbc-mysql 은 Spring Boot 3.3+ BOM 이 관리(io.r2dbc:r2dbc-mysql 의 후속).
    runtimeOnly("io.asyncer:r2dbc-mysql")
    implementation("io.r2dbc:r2dbc-pool")

    // ----- Flyway (JDBC 기반 마이그레이션) -----
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    // Flyway 가 사용할 JDBC 드라이버 (Spring Boot BOM 이 버전 관리)
    runtimeOnly("com.mysql:mysql-connector-j")

    // ----- jOOQ runtime (codegen 결과물이 참조) -----
    implementation("org.jooq:jooq")
    implementation("org.jooq:jooq-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // ----- jOOQ codegen 전용 의존성 (JDBC 드라이버만) -----
    // plugin 의 `jooqCodegen` configuration 으로 코드 생성 시 필요한 드라이버를 주입.
    jooqCodegen("com.mysql:mysql-connector-j:8.4.0")

    // ----- Test -----
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:r2dbc")
}

// =============================================================================
// jOOQ codegen 설정 (공식 plugin DSL)
// 문서: https://www.jooq.org/doc/3.20/manual/code-generation/codegen-execution/codegen-gradle/
// - 단일 execution 가정 → `jooqCodegenJooq` 태스크가 자동 생성됨.
// - codegen 은 로컬 Docker MySQL 8(3306) 에서 스키마를 introspect 한다.
//   (Testcontainers 아님 — codegen 시점에는 단순 JDBC 연결이 가장 빠르고 확실함)
// =============================================================================
jooq {
    configuration {
        jdbc {
            driver = "com.mysql.cj.jdbc.Driver"
            url = "jdbc:mysql://localhost:3306/path_enum?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8"
            user = "root"
            password = "root"
        }
        generator {
            // Kotlin 코드 생성 (3.19+ 부터 안정)
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                name = "org.jooq.meta.mysql.MySQLDatabase"
                inputSchema = "path_enum"
                // comments 테이블만 생성 (대소문자 무관 매칭)
                includes = "comments"
                // 불필요한 시스템/참조 객체 제외
                excludes = ""
            }
            target {
                packageName = "com.career.pathenum.generated"
                directory = "build/generated-jooq/src/main/java"
            }
        }
    }
}

// 생성된 Kotlin 소스를 컴파일 classpath 에 포함
sourceSets {
    main {
        kotlin.srcDir("build/generated-jooq/src/main/java")
    }
}

// compileKotlin 이 codegen 보다 먼저/같이 실행되도록 의존성 연결
// (공식 plugin 단일 execution 의 집계 태스크 이름 = `jooqCodegen`)
tasks.withType<KotlinCompile> {
    dependsOn("jooqCodegen")
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // macOS(colima/orbstack) 환경에서 Testcontainers 가 docker daemon 을 찾지 못하는 문제 해결.
    // Gradle test task 가 fork 한 JVM 은 shell 환경변수를 상속받지 않으므로 명시적으로 주입한다.
    val userHome = System.getProperty("user.home")
    val colimaSocket = "$userHome/.colima/default/docker.sock"
    val orbstackSocket = "$userHome/.orbstack/run/docker.sock"
    val resolvedHost = when {
        File(colimaSocket).exists() -> "unix://$colimaSocket"
        File(orbstackSocket).exists() -> "unix://$orbstackSocket"
        else -> System.getenv("DOCKER_HOST")
    }
    resolvedHost?.let { environment("DOCKER_HOST", it) }
    // colima 최소 지원 API 가 1.44 인데 docker-java 기본이 1.32 → 거부된다. 강제 상향.
    environment("DOCKER_API_VERSION", "1.44")
    systemProperty("api.version", "1.44")
    // Colima 가 docker desktop 마운트 파일을 제공하지 않을 수 있음 - Ryuk disabled.
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}
