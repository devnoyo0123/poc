plugins {
    java
    application
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.example.vthread.VirtualThreadPinningDemo"
    applicationDefaultJvmArgs = listOf(
        "-Djdk.virtualThreadScheduler.parallelism=1",
        "-Djdk.tracePinnedThreads=full",
    )
}

tasks.register<JavaExec>("runQuiz") {
    group = "application"
    description = "Run ReentrantLock queue quiz"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.example.vthread.quiz.ReentrantLockQuiz"
}
