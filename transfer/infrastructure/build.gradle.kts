dependencies {
    api(project(":transfer:model"))
    api(project(":common:model"))
    implementation(project(":common:security"))
    implementation(project(":account:api-internal"))
    implementation(project(":member:api-internal"))
    implementation("org.springframework:spring-web")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.2.1")
}
