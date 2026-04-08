dependencies {
    api(project(":account:model"))
    implementation(project(":account:infrastructure"))
    implementation(project(":account:exception"))
    implementation("org.springframework:spring-tx")
}
