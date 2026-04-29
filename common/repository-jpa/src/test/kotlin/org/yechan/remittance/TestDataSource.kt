package org.yechan.remittance

import java.io.PrintWriter
import java.sql.Connection
import java.sql.SQLFeatureNotSupportedException
import java.util.logging.Logger
import javax.sql.DataSource

class TestDataSource : DataSource {

    override fun getConnection(): Connection = throw SQLFeatureNotSupportedException(
        "test data source does not provide connections",
    )

    override fun getConnection(
        username: String?,
        password: String?,
    ): Connection = throw SQLFeatureNotSupportedException("test data source does not provide connections")

    override fun getLogWriter(): PrintWriter? = null

    override fun setLogWriter(out: PrintWriter?) = Unit

    override fun setLoginTimeout(seconds: Int) = Unit

    override fun getLoginTimeout(): Int = 0

    override fun getParentLogger(): Logger = Logger.getGlobal()

    override fun <T : Any?> unwrap(iface: Class<T>?): T = throw SQLFeatureNotSupportedException(
        "unwrap is not supported",
    )

    override fun isWrapperFor(iface: Class<*>?): Boolean = false
}
