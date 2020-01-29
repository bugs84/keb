package org.kebish.core

import org.openqa.selenium.NoSuchElementException
import kotlin.reflect.KProperty

/** T this is not nullable. I cannot see case, where we want return null */
class Content<T : Any>(val cache: Boolean = false, val required: Boolean = false, val initializer: () -> T) {

    private lateinit var cachedValue: T

    operator fun getValue(thisRef: Any?, prop: KProperty<*>): T {
        if (cache) {
            if (!::cachedValue.isInitialized) {
                cachedValue = initializer()
            }
            return cachedValue
        } else {
            return initializer()
        }
//            return "$thisRef, thank you for delegating '${prop.name}' to me!"
    }

    private fun checkedInitializer(): T {
        val content = initializer()
        return if (required) {
            when(content) {
                is EmptyWebElement -> throw NoSuchElementException("Required page content is not present. Selector='${content.selector}'.")
                is EmptyWebElementList -> throw NoSuchElementException("Required page content is not present. Selector='${content.selector}'.")
                else -> content
            }
        } else {
            content
        }
    }
}

fun <T : Any> content(cache: Boolean = false, required: Boolean = false, initializer: () -> T)
        = Content(cache, required, initializer)
