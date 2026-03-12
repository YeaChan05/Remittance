
import com.linecorp.support.project.multi.recipe.configureByTypeExpression
import com.linecorp.support.project.multi.recipe.configureByTypeHaving
import com.linecorp.support.project.multi.recipe.configureByTypePrefix
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    idea
    java
    `java-library`
    `jvm-test-suite`
    id("buildlogic.testcontainers-support")
    alias(libs.plugins.spotless)
    alias(libs.plugins.build.recipe)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    jacoco
}

allprojects {
    findProperty("group")?.let {
        group = it
    }
}

val developmentOnly by configurations.creating
configurations {
    runtimeClasspath {
        extendsFrom(developmentOnly)
    }
}

configureByTypePrefix("java") {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    testing {
        suites {
            val test by getting(JvmTestSuite::class)
            val integrationTest by registering(JvmTestSuite::class) {
                sources {
                    java {
                        setSrcDirs(listOf("src/integrationTest/kotlin"))
                    }
                    resources {
                        setSrcDirs(listOf("src/integrationTest/resources"))
                    }
                }
            }

            withType<JvmTestSuite> {
                useJUnitJupiter()

                targets {
                    all {
                        dependencies {
                            implementation(project())
                        }
                        testTask.configure {
                            shouldRunAfter(test)
                            testLogging {
                                events = mutableSetOf(TestLogEvent.FAILED)
                                exceptionFormat = TestExceptionFormat.FULL
                            }
                        }
                    }
                }
            }
        }
    }

    val integrationTestImplementation by configurations.getting {
        extendsFrom(configurations.testImplementation.get())
    }

    val integrationTestRuntimeOnly by configurations.getting {
        extendsFrom(configurations.testRuntimeOnly.get())
    }

    val integrationTestCompileOnly by configurations.getting
    val integrationTestAnnotationProcessor by configurations.getting

    tasks {
        val check by getting {
            dependsOn("integrationTest")
        }
    }

    dependencies {
        implementation(rootProject.libs.jspecify)
        implementation(rootProject.libs.kotlin.logging)
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)
        testImplementation(enforcedPlatform(SpringBootPlugin.BOM_COORDINATES))
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
}

configureByTypePrefix("dependencies") {
    dependencies {
        implementation(enforcedPlatform(SpringBootPlugin.BOM_COORDINATES))
    }
}

configureByTypeHaving("boot") {
    dependencies {
        implementation(enforcedPlatform(SpringBootPlugin.BOM_COORDINATES))
        implementation("org.springframework.boot:spring-boot-starter")
        implementation(rootProject.libs.kotlin.reflect)
    }
}

configureByTypeHaving("java", "boot") {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
}

configureByTypeHaving("boot", "mvc") {
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-security")
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-validation")

        implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
    }
}

configureByTypeHaving("boot", "jpa", "repository") {
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    }
}

configureByTypeHaving("boot", "application") {
    apply(plugin = "application")
    apply(plugin = "org.springframework.boot")

    dependencies {
        implementation("io.micrometer:micrometer-tracing-bridge-otel")
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        developmentOnly(enforcedPlatform(SpringBootPlugin.BOM_COORDINATES))
        developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    }
}

configureByTypeHaving("boot", "mvc", "application") {
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-web")
    }
}

// bootJar -> application only
configureByTypeExpression("^(?!.*application).*$") {
    tasks.withType<BootJar> {
        enabled = false
    }
}

val jacocoAggregationProjects = mutableListOf<Project>()

subprojects {
    apply(plugin = "jacoco")

    plugins.withType<JavaPlugin> {
        jacocoAggregationProjects.add(this@subprojects)
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    // configure package path by project name
    afterEvaluate {
        val projectType = findProperty("type")?.toString().orEmpty()
        val isApplicationModule = projectType.contains("application")
        if (!isApplicationModule) {
            tasks.withType(Jar::class.java).configureEach {
                archiveBaseName.set(project.path.trimStart(':').replace(':', '-'))
            }
        }
    }
}

spotless {
    kotlin {
        ktfmt()
        targetExclude(
            "**/build/**",
            "**/generated/**",
            "**/out/**"
        )
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(jacocoAggregationProjects.flatMap { project ->
        project.tasks.withType<Test>().toList()
    })

    val sourceSets = jacocoAggregationProjects.mapNotNull { project ->
        project.extensions.findByType(SourceSetContainer::class.java)?.findByName("main")
    }

    executionData.setFrom(jacocoAggregationProjects.map { project ->
        project.fileTree(project.layout.buildDirectory) {
            include("jacoco/*.exec")
        }
    })

    classDirectories.setFrom(sourceSets.map { it.output })
    sourceDirectories.setFrom(sourceSets.map { it.allSource.srcDirs })

    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}
