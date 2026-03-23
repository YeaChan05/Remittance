testcontainers {
    bom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    task("integrationTest") {
        use("mysql")
        use("rabbitmq")
    }
}

dependencies {
    implementation(project(":common:security"))

    implementation(project(":account:api"))
    implementation(project(":account:repository-jpa"))
    implementation(project(":account:schema"))
    implementation(project(":account:mq-rabbitmq"))

    implementation(project(":transfer:api"))
    implementation(project(":transfer:repository-jpa"))
    implementation(project(":transfer:schema"))
    implementation(project(":transfer:mq-rabbitmq"))

    implementation(project(":member:api"))
    implementation(project(":member:repository-jpa"))
    implementation(project(":member:schema"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    integrationTestImplementation(testFixtures(project(":common:application-api")))
    integrationTestImplementation(project(":account:model"))
    integrationTestImplementation(project(":account:schema"))
    integrationTestImplementation(project(":member:model"))
    integrationTestImplementation(project(":member:schema"))
    integrationTestImplementation(project(":transfer:model"))
    integrationTestImplementation(project(":transfer:schema"))
    integrationTestImplementation(project(":transfer:infrastructure"))

    runtimeOnly("com.mysql:mysql-connector-j") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }

    integrationTestRuntimeOnly("com.mysql:mysql-connector-j") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
}
