package org.yechan.remittance.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.testing.Test
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicBoolean

internal interface SharedContainerProvider {
    val key: String
    val validatedModuleNames: Set<String>

    fun runtimeDependencies(
        project: Project,
        coordinates: TestcontainersRuntimeCoordinates,
    ): List<Dependency>

    fun createRuntime(classpath: Set<File>): SharedContainerRuntime
}

internal interface SharedContainerRuntime : AutoCloseable {
    fun prepare(project: Project, taskPath: String) {}

    fun applyTo(testTask: Test, project: Project, taskPath: String)
}

internal object SharedContainerRegistry {
    private val providers = listOf(
        MySqlSharedContainerProvider,
        RabbitMqSharedContainerProvider,
    ).associateBy(SharedContainerProvider::key)

    fun resolve(containerKeys: Collection<String>): List<SharedContainerProvider> = containerKeys.map { key ->
        providers[key] ?: throw GradleException("Unknown shared testcontainer key: '$key'.")
    }
}

internal abstract class ClasspathBackedSharedContainerRuntime(
    classpath: Set<File>,
) : SharedContainerRuntime {
    private val closed = AtomicBoolean(false)
    protected val classLoader = URLClassLoader(
        classpath.map { it.toURI().toURL() }.toTypedArray(),
        javaClass.classLoader,
    )

    protected fun invoke(target: Any, methodName: String, vararg args: Any): Any? = withContextClassLoader {
        val argumentTypes = args.map {
            when (it) {
                is Int -> Integer.TYPE
                else -> it.javaClass
            }
        }.toTypedArray()
        val method = target.javaClass.getMethod(methodName, *argumentTypes)
        method.invoke(target, *args)
    }

    protected fun <T> withContextClassLoader(block: () -> T): T {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = classLoader
        return try {
            block()
        } finally {
            thread.contextClassLoader = previous
        }
    }

    final override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }

        runCatching {
            closeRuntime()
        }
        runCatching {
            cleanupResourceReaper()
        }
        runCatching {
            classLoader.close()
        }
    }

    private fun cleanupResourceReaper() {
        withContextClassLoader {
            val resourceReaperClass =
                classLoader.loadClass("org.testcontainers.utility.ResourceReaper")
            val resourceReaper = resourceReaperClass.getMethod("instance").invoke(null)

            runCatching {
                resourceReaperClass.getMethod("performCleanup").invoke(resourceReaper)
            }

            if (resourceReaper.javaClass.name == "org.testcontainers.utility.RyukResourceReaper") {
                val ryukContainerField = resourceReaper.javaClass.getDeclaredField("ryukContainer")
                ryukContainerField.isAccessible = true
                val ryukContainer = ryukContainerField.get(resourceReaper)
                invoke(ryukContainer, "stop")
            }
        }
    }

    protected abstract fun closeRuntime()
}
