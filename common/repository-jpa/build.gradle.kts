dependencies {
    implementation(project(":common:boot"))
    implementation(project(":common:model"))
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.14.1")
    implementation(libs.p6spy)
    implementation(libs.aspectj.weaver)
    implementation(libs.spring.aop)
}
