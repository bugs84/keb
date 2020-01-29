package org.kebish.core

import org.openqa.selenium.WebElement

abstract class Module(val scope: WebElement? = null) : NavigationSupport, ModuleSupport, WaitSupport {

    override lateinit var browser: Browser
}