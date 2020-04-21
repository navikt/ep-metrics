
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.70"
    `java-library`
    id("net.researchgate.release") version "2.6.0"
    `maven-publish`
}

group = "no.nav.eessi.pensjon"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.adeo.no/repository/maven-central")
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

java {
    withJavadocJar()
    withSourcesJar()
}

// https://github.com/researchgate/gradle-release
release {
    newVersionCommitMessage = "[Release Plugin] - new version commit: "
    tagTemplate = "release-\${version}"
}

// https://help.github.com/en/actions/language-and-framework-guides/publishing-java-packages-with-gradle#publishing-packages-to-github-packages
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/eessi-pensjon-shared")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
