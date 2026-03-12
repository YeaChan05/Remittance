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
    implementation("org.testcontainers:testcontainers:1.21.3")
    implementation("org.testcontainers:mysql:1.21.3")
    implementation("org.testcontainers:rabbitmq:1.21.3")
    implementation("com.mysql:mysql-connector-j:9.3.0")
}

gradlePlugin {
    plugins {
        register("integrationTestEnvironment") {
            id = "remittance.integration-test-environment"
            implementationClass = "org.yechan.remittance.buildlogic.IntegrationTestEnvironmentPlugin"
        }

        register("sharedTestcontainers") {
            id = "remittance.shared-testcontainers"
            implementationClass = "org.yechan.remittance.buildlogic.IntegrationTestEnvironmentPlugin"
        }
    }
}
