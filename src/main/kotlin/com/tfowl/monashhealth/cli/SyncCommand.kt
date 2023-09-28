package com.tfowl.monashhealth.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onFailure
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import com.tfowl.gcal.GoogleApiServiceConfig
import com.tfowl.gcal.GoogleCalendar
import com.tfowl.gcal.calendarView
import com.tfowl.gcal.sync
import com.tfowl.monashhealth.*
import java.io.File
import java.io.Reader
import java.time.LocalDate

class SyncCommand : CliktCommand(name = "sync") {
    private val calendarId by googleCalendarOption().required()
    private val googleClientSecrets by googleClientSecretsOption().required()

    private val headless by option().flag("--no-headless", default = true)

    private val syncFrom by option("--sync-from")
        .convert("DATE") { it.toLocalDateOrNull() ?: fail("Invalid format: $it") }
        .default(LocalDate.now(), "today")

    private val syncTo by option("--sync-to")
        .convert("DATE") { it.toLocalDateOrNull() ?: fail("Invalid format: $it") }
        .default(LocalDate.now().plusMonths(6), "6 months from today")

    private val username by option("--username", envvar = "MH_USERNAME").required()

    private val password by option("--password", envvar = "MH_PASSWORD").required()

    override fun run() {
        println("Creating Google Calendar service")
        val service = GoogleCalendar.create(
            GoogleApiServiceConfig(
                secretsProvider = { googleClientSecrets },
                applicationName = "APPLICATION_NAME_PLACEHOLDER",
                scopes = listOf(CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_EVENTS),
                dataStoreFactory = FileDataStoreFactory(File(".monashhealth-schedule"))
            )
        )

        val calendar = service.calendarView(calendarId)

        val events = binding {
            println("Creating web driver")
            val response = createWebDriver().bind().use { pw ->
                println("Launching browser")
                val browser = launchBrowser(pw, headless = headless).bind()

                println("Logging into kronos")
                val page = login(browser, username, password).bind()

                println("Fetching events")
                requestEventsJson(
                    page, EventsRequest(syncFrom, syncTo, EventType.ALL)
                ).bind()
            }

            JSON.tryDecodeFromString<Events>(response)
                .onFailure { it.printStackTrace(); println(response) }
                .bind()
        }.getOrThrow()

        println("Transforming events")
        val roster = events.mapNotNull { it.toGoogleEventOrNull() }

        println("Synchronising to Google Calendar")
        sync(
            service, calendar,
            syncFrom..syncTo,
            roster,
            ZONE_MELBOURNE,
            DOMAIN
        )

        println("Done")
    }
}