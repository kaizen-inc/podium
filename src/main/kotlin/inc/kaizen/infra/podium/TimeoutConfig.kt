package inc.kaizen.infra.podium

import java.util.concurrent.TimeUnit

/**
 * Configuration for OkHttp client timeouts.
 *
 * @property connectTimeout The timeout for establishing a connection, in the given [timeUnit].
 * @property readTimeout The timeout for reading a response, in the given [timeUnit].
 * @property writeTimeout The timeout for sending a request, in the given [timeUnit].
 * @property timeUnit The time unit for all timeout values. Defaults to [TimeUnit.SECONDS].
 */
data class TimeoutConfig(
    val connectTimeout: Long = 30,
    val readTimeout: Long = 30,
    val writeTimeout: Long = 30,
    val timeUnit: TimeUnit = TimeUnit.SECONDS
)

