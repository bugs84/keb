package org.kebish.core

import org.openqa.selenium.WebElement

abstract class Module(val scope: WebElement? = null) : ContentSupport, ModuleSupport, NavigationSupport, WaitSupport {

    override lateinit var browser: Browser

    override fun getDefaultScope(): WebElement? {
        return scope
    }
}