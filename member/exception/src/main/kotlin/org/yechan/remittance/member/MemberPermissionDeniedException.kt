package org.yechan.remittance.member

import org.yechan.remittance.BusinessException
import org.yechan.remittance.Status

class MemberPermissionDeniedException : BusinessException {
    constructor(message: String) : super(message)

    constructor(status: Status, message: String) : super(status, message)
}
