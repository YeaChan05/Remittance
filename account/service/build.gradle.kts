dependencies {
    api(project(":account:model"))
    api(project(":common:model"))
    implementation(project(":account:infrastructure"))
    implementation(project(":account:exception"))
    implementation("org.springframework:spring-tx")
}
