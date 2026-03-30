testcontainers {
    bom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    task("integrationTest") {
        stack("aggregate")
        use("mysql")
        use("rabbitmq")
    }
}

dependencies {
    implementation(project(":common:security"))

    implementation(project(":account:api"))
    implementation(project(":account:api-internal"))
    implementation(project(":account:repository-jpa"))
    implementation(project(":account:schema"))
    implementation(project(":account:mq-rabbitmq"))

    implementation(project(":transfer:api"))
    implementation(project(":transfer:repository-jpa"))
    implementation(project(":transfer:schema"))
    implementation(project(":transfer:mq-rabbitmq"))

    implementation(project(":member:api"))
    implementation(project(":member:api-internal"))
    implementation(project(":member:repository-jpa"))
    implementation(project(":member:schema"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    developmentOnly("org.testcontainers:testcontainers-jdbc")
    developmentOnly("org.testcontainers:testcontainers-mysql")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    integrationTestImplementation(testFixtures(project(":common:application-api")))
    integrationTestImplementation(project(":account:service"))
    integrationTestImplementation(project(":member:service"))
    integrationTestImplementation(project(":transfer:service"))
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
