plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin") version "0.2.1"
}

group = "Arcadia.ClexaGod.arcadia"
description = "Arcadia core plugin for AllayMC"
version = "0.1.0-SNAPSHOT"

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        configureEach {
            options.isFork = true
        }
    }

    withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

allay {
    api = "0.23.0-SNAPSHOT"

    plugin {
        entrance = ".ArcadiaCore"
        authors += "ClexaGod"
    }
}

dependencies {
}
