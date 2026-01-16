plugins {
    id("java-library")
    id("org.allaymc.gradle.plugin") version "0.2.1"
}

group = "Arcadia.ClexaGod.arcadia"
description = "Arcadia core plugin for AllayMC"
version = "0.2.0"

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

    named<Jar>("jar") {
        archiveClassifier.set("")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        from(sourceSets.main.get().output)
        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith(".jar") }
                .map { zipTree(it) }
        })
    }

    named("build") {
        dependsOn("jar")
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
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    implementation("com.zaxxer:HikariCP:5.1.0")
    runtimeOnly("org.postgresql:postgresql:42.7.3")
}
