
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":engine"))
    testImplementation(kotlin("test"))
}
