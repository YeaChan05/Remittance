rootProject.name = "remittance"

pluginManagement {
    includeBuild("build-logic")

    buildscript {
        repositories {
            gradlePluginPortal()
        }
    }

    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":account:api")
include(":account:api-internal")
include(":account:model")
include(":account:service")
include(":account:exception")
include(":account:infrastructure")
include(":account:repository-jpa")
include(":account:schema")
include(":account:mq-rabbitmq")

include(":transfer:api")
include(":transfer:model")
include(":transfer:service")
include(":transfer:exception")
include(":transfer:infrastructure")
include(":transfer:repository-jpa")
include(":transfer:schema")
include(":transfer:mq-rabbitmq")

include(":member:api")
include(":member:api-internal")
include(":member:model")
include(":member:service")
include(":member:exception")
include(":member:infrastructure")
include(":member:repository-jpa")
include(":member:schema")

include(":auth:api")
include(":auth:service")
include(":auth:exception")
include(":auth:infrastructure")

include(":common:application-api")
include(":common:api")
include(":common:boot")
include(":common:exception")
include(":common:repository-jpa")
include(":common:security")

include(":aggregate")
