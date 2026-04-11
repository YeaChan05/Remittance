plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("testcontainersAddon") {
            id = "buildlogic.testcontainers-support"
            implementationClass = "org.yechan.remittance.buildlogic.TestcontainersPlugin"
        }

        register("testcontainersAddonLegacy") {
            id = "buildlogic.testcontainers-addon"
            implementationClass = "org.yechan.remittance.buildlogic.TestcontainersPlugin"
        }

        register("sharedTestcontainersLegacy") {
            id = "buildlogic.shared-testcontainers"
            implementationClass = "org.yechan.remittance.buildlogic.TestcontainersPlugin"
        }

        register("integrationTestEnvironmentLegacy") {
            id = "remittance.integration-test-environment"
            implementationClass = "org.yechan.remittance.buildlogic.TestcontainersPlugin"
        }

        register("remittanceSharedTestcontainersLegacy") {
            id = "remittance.shared-testcontainers"
            implementationClass = "org.yechan.remittance.buildlogic.TestcontainersPlugin"
        }
    }
}
