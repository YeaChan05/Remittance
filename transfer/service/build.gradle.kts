dependencies {
    api(project(":transfer:model"))
    api(project(":common:model"))
    implementation(project(":transfer:infrastructure"))
    implementation(project(":transfer:exception"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    // spring tx
    implementation("org.springframework:spring-tx")
}
