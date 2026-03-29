testcontainers {
    bom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    task("integrationTest") {
        stack("transfer")
        use("mysql")
    }
}

dependencies {
    implementation(project(":common:repository-jpa"))
    implementation(project(":transfer:infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-liquibase")
    integrationTestImplementation(project(":transfer:schema"))
    integrationTestImplementation(testFixtures(project(":common:application-api")))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    integrationTestRuntimeOnly(enforcedPlatform("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}"))
    integrationTestRuntimeOnly("org.testcontainers:testcontainers-jdbc")
    integrationTestRuntimeOnly("org.testcontainers:testcontainers-mysql")
    integrationTestRuntimeOnly("com.mysql:mysql-connector-j") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
}
