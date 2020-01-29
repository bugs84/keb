package org.kebish.core

import org.openqa.selenium.WebElement

interface WaitSupport {

    val browser: Browser

    val defaultWaitPreset get() = browser.config.getDefaultPreset()

    fun <T> waitFor(
        preset: String? = null,
        desc: String? = null,
        f: () -> T?
    ): T {
        val customOrDefaultPreset = preset?.let { browser.config.getWaitPreset(it) } ?: defaultWaitPreset
        return waitFor(customOrDefaultPreset.timeout, customOrDefaultPreset.retryInterval, desc, f)
    }

    fun <T> waitFor(
        timeout: Number = defaultWaitPreset.timeout,
        retryInterval: Number = defaultWaitPreset.retryInterval,
        desc: String? = null,
        f: () -> T?
    ): T {
        val timeoutMillis = timeout.toMillis()
        val retryIntervalMillis = retryInterval.toMillis()
        val timeoutAt = System.currentTimeMillis() + timeoutMillis
        var passed = false
        var value: T? = null
        var thrown: Throwable? = null

        try {
            value = f()
            passed = resolveTruthiness(value)
        } catch (t: Throwable) {
            thrown = t
        }

        var timedOut = System.currentTimeMillis() > timeoutAt
        while (!passed && !timedOut) {
            Thread.sleep(retryIntervalMillis)
            try {
                value = f()
                passed = resolveTruthiness(value)
                thrown = null
            } catch (t: Throwable) {
                thrown = t
            } finally {
                timedOut = System.currentTimeMillis() > timeoutAt
            }
        }

        return if (passed) {
            value!!
        } else {
            val err = WaitTimeoutMessageBuilder(timeout)
                .withDetail(desc)
                .withLastEvaluatedValue(value)
                .withLastThrown(thrown)
                .build()
            throw WaitTimeoutException(err, thrown)
        }
    }

    private fun Number.toMillis() = toDouble().times(1000).toLong()

    private fun resolveTruthiness(value: Any?): Boolean {
        return when (value) {
            is Number -> value != 0
            is CharSequence -> value.length > 0
            is Boolean -> value
            is Collection<*> -> if (value.isEmpty()) false else value.all { resolveTruthiness(it) }
            is WebElement -> value.location != null
            is Module -> value.scope?.let { resolveTruthiness(it) } ?: true
            is EmptyContent, null -> false
            else -> true
        }
    }

}

class WaitTimeoutMessageBuilder(private val timeoutedAfter: Number) {

    private var detail: String = ""
    private var lastEvaluatedValue: String = ""
    private var lastThrowableMessage = ""

    fun withDetail(detail: String?) = apply {
        detail?.let { this.detail = " for '$it'" }
    }

    fun withLastEvaluatedValue(lastEvaluatedValue: Any?) = apply {
        this.lastEvaluatedValue = " Last evaluated value: '$lastEvaluatedValue'."
    }

    fun withLastThrown(lastThrown: Throwable?) = apply {
        lastThrown?.let { this.lastThrowableMessage = " Last exception cause: '${it.message}'." }
    }

    fun build() =
        "Waiting$detail has timed out after $timeoutedAfter seconds.$lastEvaluatedValue$lastThrowableMessage"

}

data class WaitPreset(val timeout: Number, val retryInterval: Number)

class WaitTimeoutException(msg: String, cause: Throwable?) : RuntimeException(msg, cause)
class WaitPresetNotFoundException(presetName: String) : RuntimeException("Preset with name '$presetName' not found.")