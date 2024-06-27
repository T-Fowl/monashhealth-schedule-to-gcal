package com.tfowl.monashhealth

import com.github.michaelbull.result.Result
import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.AriaRole
import com.microsoft.playwright.options.RequestOptions
import kotlinx.serialization.encodeToString

fun createWebDriver(): Result<Playwright, Throwable> =
    com.github.michaelbull.result.runCatching {
        Playwright.create(
            Playwright.CreateOptions().setEnv(
                mapOf(
                    "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD" to "true"
                )
            )
        )
    }

fun connectToBrowser(playwright: Playwright, url: String): Result<Browser, Throwable> =
    com.github.michaelbull.result.runCatching {
        playwright.chromium().connect(url)
    }

fun login(browser: Browser, username: String, password: String): Result<Page, Throwable> =
    com.github.michaelbull.result.runCatching {
        val page = browser.newPage(Browser.NewPageOptions())
        page.navigate("https://monashhealth-SSO.prd.mykronos.com")

        page.getByPlaceholder("Username").fill(username)
        page.getByPlaceholder("Password").fill(password)
        page.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName("Sign in")).click()

        page.waitForURL("https://monashhealth-sso.prd.mykronos.com/wfd/home")

        page
    }

fun requestEventsJson(page: Page, request: EventsRequest): Result<String, Throwable> =
    com.github.michaelbull.result.runCatching {
        val xsrfToken = page.context().cookies().first { it.name == "XSRF-TOKEN" }.value

        val req = page.request().post(
            "https://monashhealth-sso.prd.mykronos.com/myschedule/eventDispatcher",
            RequestOptions.create().setHeader("x-xsrf-token", xsrfToken)
                .setHeader("Content-Type", "application/json").setData(JSON.encodeToString(request))
        )

        if (req.ok()) {
            req.body().decodeToString()
        } else {
            throw IllegalStateException(req.statusText())
        }
    }