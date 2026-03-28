testcontainers {
    bom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    task("integrationTest") {
        stack("transfer")
        use("rabbitmq")
    }
}

dependencies {
    implementation(project(":common:security"))
    implementation(project(":transfer:api"))
    implementation(project(":transfer:infrastructure"))
    implementation(project(":transfer:repository-jpa"))
    implementation(project(":transfer:schema"))
    implementation(project(":transfer:mq-rabbitmq"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    developmentOnly("org.testcontainers:testcontainers-jdbc")
    developmentOnly("org.testcontainers:testcontainers-mysql")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    integrationTestImplementation(testFixtures(project(":common:application-api")))
    integrationTestRuntimeOnly(enforcedPlatform("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}"))
    integrationTestRuntimeOnly("org.testcontainers:testcontainers-jdbc")
    integrationTestRuntimeOnly("org.testcontainers:testcontainers-mysql")
    integrationTestRuntimeOnly("com.mysql:mysql-connector-j") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    runtimeOnly("com.mysql:mysql-connector-j") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
}
