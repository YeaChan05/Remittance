plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
}

gradlePlugin {
    plugins {
        register("testcontainersAddon") {
            id = "buildlogic.testcontainers-support"
            implementationClass = "org.yechan.remittance.buildlogic.IntegrationTestEnvironmentPlugin"
        }

        register("testcontainersAddonLegacy") {
            id = "buildlogic.testcontainers-addon"
            implementationClass = "org.yechan.remittance.buildlogic.IntegrationTestEnvironmentPlugin"
        }

        register("sharedTestcontainersLegacy") {
            id = "buildlogic.shared-testcontainers"
            implementationClass = "org.yechan.remittance.buildlogic.IntegrationTestEnvironmentPlugin"
        }

        register("integrationTestEnvironmentLegacy") {
            id = "remittance.integration-test-environment"
            implementationClass = "org.yechan.remittance.buildlogic.IntegrationTestEnvironmentPlugin"
        }

        register("remittanceSharedTestcontainersLegacy") {
            id = "remittance.shared-testcontainers"
            implementationClass = "org.yechan.remittance.buildlogic.IntegrationTestEnvironmentPlugin"
        }
    }
}
