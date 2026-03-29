testcontainers {
    bom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    task("integrationTest") {
        stack("account")
        use("mysql")
    }
}

dependencies {
    implementation(project(":common:repository-jpa"))
    implementation(project(":account:infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-liquibase")
    integrationTestImplementation(project(":account:schema"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    integrationTestRuntimeOnly(enforcedPlatform("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}"))
    integrationTestRuntimeOnly("org.testcontainers:testcontainers-jdbc")
    integrationTestRuntimeOnly("org.testcontainers:testcontainers-mysql")
    integrationTestRuntimeOnly("com.mysql:mysql-connector-j") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
}
