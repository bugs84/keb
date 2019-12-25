package com.horcacorp.testing.keb.core

import org.openqa.selenium.WebElement
import java.net.URI

class Browser(val config: Configuration) : ContentSupport, NavigationSupport, WaitSupport, ModuleSupport {

    companion object {
        fun drive(config: Configuration = Configuration(), block: Browser.() -> Unit) {
            val browser = Browser(config)
            try {
                block(browser)
            } finally {
                browser.quit()
            }
        }
    }

    val driver = config.driver ?: throw IllegalStateException("Browser is not initialized.")

    override fun css(
        selector: String,
        scope: WebElement?,
        fetch: ContentFetchType?
    ): WebElement =
        WebElementDelegate(
            scope?.let { ScopedCssSelector(selector, it) } ?: CssSelector(selector, driver),
            fetch ?: config.elementsFetchType
        )

    override fun cssList(
        selector: String,
        scope: WebElement?,
        fetch: ContentFetchType?
    ): List<WebElement> =
        WebElementsListDelegate(
            scope?.let { ScopedCssSelector(selector, it) } ?: CssSelector(selector, driver),
            fetch ?: config.elementsFetchType
        )

    override fun html(tag: String, scope: WebElement?, fetch: ContentFetchType?): WebElement =
        WebElementDelegate(
            scope?.let { ScopedHtmlSelector(tag, it) } ?: HtmlSelector(tag, driver),
            fetch ?: config.elementsFetchType
        )

    override fun htmlList(
        tag: String,
        scope: WebElement?,
        fetch: ContentFetchType?
    ): List<WebElement> =
        WebElementsListDelegate(
            scope?.let { ScopedHtmlSelector(tag, it) } ?: HtmlSelector(tag, driver),
            fetch ?: config.elementsFetchType
        )

    override fun xpath(
        xpath: String,
        scope: WebElement?,
        fetch: ContentFetchType?
    ): WebElement =
        WebElementDelegate(
            scope?.let { ScopedXpathSelector(xpath, it) } ?: XPathSelector(xpath, driver),
            fetch ?: config.elementsFetchType
        )

    override fun xpathList(
        xpath: String,
        scope: WebElement?,
        fetch: ContentFetchType?
    ): List<WebElement> =
        WebElementsListDelegate(
            scope?.let { ScopedXpathSelector(xpath, it) } ?: XPathSelector(xpath, driver),
            fetch ?: config.elementsFetchType
        )

    override fun <T : Module> module(factory: (Browser) -> T): T = factory(this)

    override fun <T : ScopedModule> scopedModule(
        factory: (Browser, WebElement) -> T,
        scope: WebElement
    ): T =
        factory(this, scope)

    override fun <T> waitFor(presetName: String?, desc: String?, f: () -> T): T {
        val preset = presetName
            ?.let { config.waitPresets[it.toUpperCase()] ?: throw WaitPresetNotFoundException(it) }
            ?: config.getDefaultPreset()
        return waitFor(preset.timeoutMillis, preset.retryIntervalMillis, desc, f)
    }

    override fun <T> waitFor(timeoutMillis: Long, retryIntervalMillis: Long, desc: String?, f: () -> T): T {
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
            val err = if (desc != null) {
                "Waiting for '$desc' has timed out after $timeoutMillis milliseconds."
            } else {
                "Waiting has timed out after $timeoutMillis milliseconds."
            }
            throw WaitTimeoutException(err, thrown)
        }
    }

    override fun <T : Page> to(pageFactory: (Browser) -> T, waitPreset: String?, body: T.() -> Unit): T =
        to(pageFactory(this), waitPreset, body)

    override fun <T : Page> at(pageFactory: (Browser) -> T, waitPreset: String?, body: T.() -> Unit): T =
        at(pageFactory(this), waitPreset, body)


    private fun <T : Page> to(page: T, waitPreset: String?, body: T.() -> Unit): T {
        driver.get(resolveUrl(page.url()))
        return at(page, waitPreset, body)
    }

    private fun <T : Page> at(page: T, waitPreset: String?, body: T.() -> Unit): T {
        page.verifyAt(waitPreset)
        body.invoke(page)
        return page
    }

    override fun <T> withNewTab(action: () -> T): T {
        val currentTabIndex = driver.windowHandles.indexOf(driver.windowHandle)
        val r = action()
        val nextTabHandle = driver.windowHandles.elementAt(currentTabIndex + 1)
        driver.switchTo().window(nextTabHandle)
        return r
    }

    override fun <T> withClosedTab(action: () -> T): T {
        val currentTabIndex = driver.windowHandles.indexOf(driver.windowHandle)
        val r = action()
        val previous = driver.windowHandles.elementAt(currentTabIndex - 1)
        driver.switchTo().window(previous)
        return r
    }

    fun quit() {
        driver.quit()
    }

    private fun resolveUrl(urlSuffix: String): String {
        val urlPrefix = config.baseUrl
        val url = if (urlPrefix.isEmpty()) urlSuffix else "$urlPrefix/$urlSuffix"
        return URI(url).normalize().toString()
    }

    private fun resolveTruthiness(value: Any?): Boolean {
        return when (value) {
            is Number -> value != 0
            is CharSequence -> value.length > 0
            is Boolean -> value
            is Collection<*> -> if (value.isEmpty()) false else value.all { resolveTruthiness(it) }
            is WebElement -> value.location != null
            null -> false
            else -> true
        }
    }

}