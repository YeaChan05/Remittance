testcontainers {
    bom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    task("integrationTest") {
        stack("account")
        use("mysql")
        liquibase("classpath:/db/changelog/db.changelog-master.yaml")
    }
}

dependencies {
    implementation(project(":common:security"))
    implementation(project(":account:api"))
    implementation(project(":account:repository-jpa"))
    implementation(project(":account:schema"))
    implementation(project(":account:mq-rabbitmq"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    developmentOnly("org.testcontainers:testcontainers-jdbc")
    developmentOnly("org.testcontainers:testcontainers-mysql")

    integrationTestImplementation(project(":account:api-internal"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    integrationTestImplementation(testFixtures(project(":common:application-api")))
    integrationTestRuntimeOnly(enforcedPlatform("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}"))

    runtimeOnly("com.mysql:mysql-connector-j") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
}
