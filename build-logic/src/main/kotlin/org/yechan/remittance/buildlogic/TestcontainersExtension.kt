package org.yechan.remittance.buildlogic

enum class TestContainerResource {
    MYSQL,
    RABBITMQ
}

open class TestcontainersExtension {
    internal val resources: MutableSet<TestContainerResource> = linkedSetOf()
    internal var bomCoordinate: String? = null

    fun mysql() {
        use(TestContainerResource.MYSQL)
    }

    fun rabbitMq() {
        use(TestContainerResource.RABBITMQ)
    }

    fun use(resource: TestContainerResource) {
        resources += resource
    }

    fun bom(coordinate: String) {
        bomCoordinate = coordinate
    }
}
