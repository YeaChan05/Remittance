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

    runtimeOnly("com.mysql:mysql-connector-j") {
        exclude(group = "com.google.protobuf", module = "protobuf-java")
    }
}
