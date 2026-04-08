package org.yechan.remittance.transfer

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.core.env.Environment
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.yechan.remittance.InternalServiceAuthProperties
import org.yechan.remittance.InternalServiceAuthenticationFilter
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import org.yechan.remittance.member.internal.contract.MemberExistenceInternalApi

@AutoConfiguration
class TransferInfrastructureBeanRegistrar :
    BeanRegistrarDsl({
        if (env.containsProperty("spring.http.serviceclient.account-internal.base-url")) {
            registerBean<AccountInternalApi> {
                createInternalApiClient<AccountInternalApi>(
                    environment = bean(),
                    internalServiceAuthProperties = bean(),
                    groupName = "account-internal",
                )
            }
        }

        if (env.containsProperty("spring.http.serviceclient.member-internal.base-url")) {
            registerBean<MemberExistenceInternalApi> {
                createInternalApiClient<MemberExistenceInternalApi>(
                    environment = bean(),
                    internalServiceAuthProperties = bean(),
                    groupName = "member-internal",
                )
            }
        }

        registerBean<TransferAccountClient> {
            TransferAccountClientAdapter(bean())
        }

        registerBean<TransferMemberClient> {
            TransferMemberClientAdapter(bean())
        }
    })

private inline fun <reified T : Any> createInternalApiClient(
    environment: Environment,
    internalServiceAuthProperties: InternalServiceAuthProperties,
    groupName: String,
): T {
    val restClient = RestClient.builder()
        .baseUrl(environment.getRequiredProperty("spring.http.serviceclient.$groupName.base-url"))
        .defaultHeader(
            InternalServiceAuthenticationFilter.INTERNAL_TOKEN_HEADER,
            internalServiceAuthProperties.token,
        )
        .build()

    val factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
    return factory.createClient(T::class.java)
}
