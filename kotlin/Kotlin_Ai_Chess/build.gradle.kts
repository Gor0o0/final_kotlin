
plugins {
    // Apply the common Kotlin JVM plugin to the root project.
    // This allows for common configurations like Kotlin version to be defined once.
    kotlin("jvm") version "1.9.0" apply false
}

// Define common dependencies or configurations here if needed for all subprojects.
// For a multi-module project, it's often better to define dependencies within subproject build files.
