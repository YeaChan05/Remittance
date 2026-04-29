package org.yechan.remittance

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

@Aspect
class P6SpySqlLogSuppressAspect {

    @Around("@annotation(SuppressP6SpySqlLog) || @within(SuppressP6SpySqlLog)")
    fun suppress(joinPoint: ProceedingJoinPoint): Any? = P6SpySqlLogSuppressor.suppress {
        joinPoint.proceed()
    }
}
