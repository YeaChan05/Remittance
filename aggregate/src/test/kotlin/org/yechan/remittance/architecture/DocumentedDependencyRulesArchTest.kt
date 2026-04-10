package org.yechan.remittance.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.Dependency
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test
import org.yechan.remittance.BusinessException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.streams.asSequence

class DocumentedDependencyRulesArchTest {
    @Test
    fun `model 모듈은 다른 내부 구현 모듈에 의존하지 않는다`() {
        classes()
            .that(areInModule("model"))
            .should(
                forbidInternalDependencies("model은 다른 구현 모듈에 의존할 수 없다") { _, target, _ ->
                    target.domain in DOMAIN_MODULES && target.module != "model"
                },
            ).check(importedClasses)
    }

    @Test
    fun `service 모듈은 own infrastructure 외 다른 구현 모듈에 직접 의존하지 않는다`() {
        classes()
            .that(areInModule("service"))
            .should(
                forbidInternalDependencies("service는 own infrastructure/model/exception/common:security 외 경계를 직접 참조할 수 없다") { source, target, dependency ->
                    target.domain in DOMAIN_MODULES &&
                        (
                            target.module in DISALLOWED_FOR_SERVICE ||
                                (target.module == "infrastructure" && target.domain != source.domain) ||
                                (target.module == "service" && target.domain != source.domain) ||
                                dependency.targetClass.packageName.contains(".internal.")
                            )
                },
            ).check(importedClasses)
    }

    @Test
    fun `api 모듈은 repository나 infrastructure 구현을 직접 참조하지 않는다`() {
        classes()
            .that(areInModule("api"))
            .should(
                forbidInternalDependencies("api는 repository-jpa나 infrastructure를 직접 참조할 수 없다") { _, target, _ ->
                    target.domain in DOMAIN_MODULES &&
                        (target.module == "repository-jpa" || target.module == "infrastructure")
                },
            ).check(importedClasses)
    }

    @Test
    fun `infrastructure 모듈은 provider internal adapter가 아니라 internal contract만 참조한다`() {
        noClasses()
            .that(areInModule("infrastructure"))
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..internal.adapter..")
            .check(importedClasses)
    }

    @Test
    fun `repository-jpa 모듈은 service나 api나 application을 직접 참조하지 않는다`() {
        classes()
            .that(areInModule("repository-jpa"))
            .should(
                forbidInternalDependencies("repository-jpa는 service/api/application을 직접 참조할 수 없다") { _, target, _ ->
                    target.domain in DOMAIN_MODULES &&
                        target.module in setOf("service", "api", "api-internal", "application")
                },
            ).check(importedClasses)
    }

    @Test
    fun `application과 aggregate는 service를 직접 참조하지 않는다`() {
        classes()
            .that(areInAnyModule("application", "aggregate"))
            .should(
                forbidInternalDependencies("application/aggregate는 business logic 구현 모듈을 직접 참조하지 않는다") { source, target, _ ->
                    when {
                        source.module == "aggregate" -> target.domain in DOMAIN_MODULES && target.module == "service"
                        source.module == "application" -> target.domain in DOMAIN_MODULES && target.module == "service"
                        else -> false
                    }
                },
            ).check(importedClasses)
    }

    @Test
    fun `internal contract는 adapter나 service 구현을 직접 참조하지 않는다`() {
        classes()
            .that()
            .resideInAnyPackage("..internal.contract..")
            .should(
                forbidInternalDependencies("internal.contract는 internal.adapter/service/repository/application으로 흐르면 안 된다") { _, target, dependency ->
                    dependency.targetClass.packageName.contains(".internal.adapter.") ||
                        (
                            target.domain in DOMAIN_MODULES &&
                                target.module in setOf(
                                    "service",
                                    "repository-jpa",
                                    "application",
                                )
                            )
                },
            ).check(importedClasses)
    }

    @Test
    fun `도메인 exception은 BusinessException 계층을 따른다`() {
        classes()
            .that(areInModule("exception"))
            .and().haveSimpleNameEndingWith("Exception")
            .should().beAssignableTo(BusinessException::class.java)
            .check(importedClasses)
    }

    companion object {
        private val importedClasses: JavaClasses by lazy {
            ClassFileImporter().importPaths(mainOutputDirectories())
        }

        private val DOMAIN_MODULES = setOf("account", "member", "transfer")
        private val DISALLOWED_FOR_SERVICE =
            setOf("repository-jpa", "api", "api-internal", "application", "mq-rabbitmq", "schema")

        private fun mainOutputDirectories(): List<Path> {
            val root = findRepositoryRoot()

            return Files.walk(root).use { paths ->
                paths.asSequence()
                    .filter(Files::isDirectory)
                    .filter {
                        it.endsWith("build/classes/kotlin/main") ||
                            it.endsWith("build/classes/java/main")
                    }.toList()
            }
        }

        private fun findRepositoryRoot(): Path = generateSequence(Paths.get("").toAbsolutePath().normalize()) { it.parent }
            .first { it.resolve("settings.gradle.kts").exists() }

        private fun areInModule(module: String): DescribedPredicate<JavaClass> = object : DescribedPredicate<JavaClass>("classes in $module module") {
            override fun test(input: JavaClass): Boolean = input.moduleRef()?.module == module
        }

        private fun areInAnyModule(vararg modules: String): DescribedPredicate<JavaClass> = object : DescribedPredicate<JavaClass>("classes in modules ${modules.joinToString()}") {
            override fun test(input: JavaClass): Boolean = input.moduleRef()?.module in modules.toSet()
        }

        private fun forbidInternalDependencies(
            description: String,
            violation: (source: ModuleRef, target: ModuleRef, dependency: Dependency) -> Boolean,
        ): ArchCondition<JavaClass> = object : ArchCondition<JavaClass>(description) {
            override fun check(item: JavaClass, events: ConditionEvents) {
                val source = item.moduleRef() ?: return

                item.directDependenciesFromSelf.forEach { dependency ->
                    val target = dependency.targetClass.moduleRef() ?: return@forEach

                    if (violation(source, target, dependency)) {
                        events.add(
                            SimpleConditionEvent.violated(
                                dependency,
                                "${item.fullName} (${source.domain}:${source.module}) -> " +
                                    "${dependency.targetClass.fullName} (${target.domain}:${target.module})",
                            ),
                        )
                    }
                }
            }
        }

        private fun JavaClass.moduleRef(): ModuleRef? = source.orElse(null)?.uri?.toString()?.let(::parseModuleRef)

        private fun parseModuleRef(location: String): ModuleRef? {
            val normalized = location.replace('\\', '/')

            Regex(""".*/aggregate/build/classes/(?:kotlin|java)/main/.*""")
                .matchEntire(normalized)
                ?.let { return ModuleRef("aggregate", "aggregate") }

            val match =
                Regex(""".*/(account|member|transfer|common)/([^/]+)/build/classes/(?:kotlin|java)/main/.*""")
                    .matchEntire(normalized)
                    ?: return null

            return ModuleRef(
                domain = match.groupValues[1],
                module = match.groupValues[2],
            )
        }
    }
}

private data class ModuleRef(
    val domain: String,
    val module: String,
)
