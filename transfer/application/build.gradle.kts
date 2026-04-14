testcontainers {
    bom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    task("integrationTest") {
        stack("transfer")
        use("mysql")
        use("rabbitmq")
        liquibase("classpath:/db/changelog/db.changelog-master.yaml")
    }
}

dependencies {
    implementation(project(":common:security"))
    implementation(project(":transfer:api"))
    implementation(project(":transfer:repository-jpa"))
    implementation(project(":transfer:schema"))
    implementation(project(":transfer:mq-rabbitmq"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    developmentOnly("org.testcontainers:testcontainers-jdbc")
    developmentOnly("org.testcontainers:testcontainers-mysql")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    integrationTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    integrationTestImplementation(testFixtures(project(":common:application-api")))
    integrationTestImplementation(project(":transfer:infrastructure"))
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
