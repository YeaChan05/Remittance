dependencies {
    implementation(project(":common:boot"))
    implementation(project(":transfer:infrastructure"))
    implementation(project(":transfer:service"))
    implementation("org.springframework.boot:spring-boot-starter-amqp")
}
