plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.github.ignaciotcrespo"
version = "1.3.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2022.3")
    type.set("IC")
    updateSinceUntilBuild.set(false)
}

tasks {
    patchPluginXml {
        sinceBuild.set("223")
    }

    buildSearchableOptions {
        enabled = false
    }
}
