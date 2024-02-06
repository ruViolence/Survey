plugins {
    `java-library`
    id("java")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.freefair.lombok") version "8.0.1"
}

group = "ru.violence.survey"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://maven.pkg.github.com/ruViolence/Reaper")
    maven("https://maven.pkg.github.com/ruViolence/CoreAPI")
}

dependencies {
    compileOnly("com.github.ruviolence:reaper:1.12.2-R0.1-SNAPSHOT")
    compileOnly("ru.violence:coreapi-common:0.1.14") {
        isTransitive = false
    }
    compileOnly("ru.violence:coreapi-bukkit:0.1.14") {
        isTransitive = false
    }
    compileOnly("org.jetbrains:annotations:24.0.1")
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifact(tasks.getByName("jar")) {
                artifactId = project.name
                classifier = ""
            }
            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")
                configurations["compileClasspath"].allDependencies.forEach {
                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", it.name)
                    dependencyNode.appendNode("version", it.version)
                }
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(8)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
                "name" to project.name,
                "version" to project.version,
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveFileName.set("Survey.jar")
    }
}
