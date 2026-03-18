package org.yechan.remittance

import org.springframework.beans.factory.BeanRegistrarDsl

fun BeanRegistrarDsl.whenPropertyEnabled(
    prefix: String,
    name: String,
    havingValue: String = "true",
    matchIfMissing: Boolean = false,
    block: BeanRegistrarDsl.() -> Unit
) {
    val key = "$prefix.$name"
    val value = env.getProperty(key)
    val enabled = value?.equals(havingValue, ignoreCase = true) ?: matchIfMissing

    if (enabled) {
        block()
    }
}
