# Keb
## Page object pattern
Keb is a https://gebish.org inspired Selenium wrapper written in Kotlin that allows you to modularize pages of your web application into logic units represented by Kotlin classes.
```kotlin
class KotlinHomePage : Page() {
    override fun url() = "https://kotlinlang.org"
}
```

### Page elements
To select web element on your page use the following methods.
```kotlin
css(".my-selector") // returns single DOM element by CSS selector
cssList(".my-select") // returns list of all elements found by CSS selector
// following selectors are also available
html("h1")
htmlList("h1")
xpath("/html/body/h1")
xpathList("/html/body/h1")
```

In order to lazily access page content use the `content` delegate.
```kotlin
class KotlinHomePage : Page() {
    override fun url() = "https://kotlinlang.org"
    
    val header by content { css(".global-header-logo") }
    val headerText by content { header.text }
}
```

### Page verifier
In order to verify, that you successfully landed on your page, you can use `at()` method.
```kotlin
class KotlinHomePage : Page() {
    override fun url() = "https://kotlinlang.org"
    override fun at() = header
    
    val header by content { css(".global-header-logo") }
}
```

### Modules
If you want to reuse page content present on multiple pages, you can use modules. Module has an optional constructor
parameter `scope`, which can be used as a search root for all the module's content. To initialize module with use the `module` method.
```kotlin
class NavMenuModule(scope: WebElement) : Module(scope) {
    val menuItems by content { htmlList("a") }
}

class KotlinHomePage : Page() {
    override fun url() = "https://kotlinlang.org"
    
    val menu by content { module(NavMenuModule(css(".nav-links"))) }
}
```

## Waiting
Waiting for some page element to be present, you can use waitFor method.
```kotlin
css(".form-input").click()
val result = waitFor {
    css(".result")
}
``` 

## Usage
```kotlin
// configuration
val config = kebConfig {
    driver = FirefoxDriver()
}
Browser.drive(config) {
    val kotlinHomePage = to(::KotlinHomePage)
    kotlinHomePage.menu.first().click()
    // ...
}
```
For full usage example please refer to [/keb-core/src/test/kotlin/keb](/keb-core/src/test/kotlin/keb).