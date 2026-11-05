import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    api("com.android.tools.build:apksig:8.7.3")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    // Optional end-to-end inputs: -PtestApk=/path/to/app.apk
    project.findProperty("testApk")?.let { systemProperty("testApk", it) }
    systemProperty("testOut", layout.buildDirectory.dir("clone-test").get().asFile.absolutePath)
    testLogging { events("passed", "failed"); showStandardStreams = true }
}
