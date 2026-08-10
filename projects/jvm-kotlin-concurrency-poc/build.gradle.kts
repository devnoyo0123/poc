plugins {
    kotlin("jvm") version "1.9.24"
    application
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

// 각 예제를 개별 실행하기 위한 헬퍼 태스크.
// 사용법: ./gradlew runExample -PmainClass=com.example.concurrency.c01synchronized.SynchronizedBufferKt
tasks.register<JavaExec>("runExample") {
    group = "application"
    description = "Run a single concurrency example by -PmainClass=..."
    classpath = sourceSets["main"].runtimeClasspath
    val target = (project.findProperty("mainClass") as String?)
        ?: "com.example.concurrency.RunAllKt"
    mainClass.set(target)
}

application {
    // 기본 실행 = 전체 벤치마크 비교
    mainClass.set("com.example.concurrency.RunAllKt")
}
