testcontainers {
    bom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
    mysql()
}

dependencies {
    implementation(project(":common:repository-jpa"))
    implementation(project(":account:infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-liquibase")

    integrationTestImplementation("org.springframework.boot:spring-boot-starter-liquibase")
    integrationTestImplementation(project(":account:schema"))
    integrationTestImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    integrationTestRuntimeOnly("com.mysql:mysql-connector-j") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
}
