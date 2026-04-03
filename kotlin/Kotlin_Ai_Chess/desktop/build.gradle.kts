
plugins {
    kotlin("jvm")
    application // Apply the application plugin for executable JARs
}

dependencies {
    // Depend on the core module
    implementation(project(":core"))
    implementation(project(":engine"))
    implementation(project(":ai"))
    implementation(project(":graphics"))
    implementation(project(":shared"))

    // Example dependency for a desktop UI framework (e.g., Compose Multiplatform, TornadoFX)
    // For now, we'll just include a basic Kotlin stdlib dependency.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.kotlin_ai_chess.desktop.MainKt") // Set the main class for the desktop application
}
