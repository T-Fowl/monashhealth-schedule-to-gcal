package com.tfowl.monashhealth.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.*
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
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate

private val LOGGER = LoggerFactory.getLogger(SyncCommand::class.java)!!

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

    private val playwrightDriverUrl by option("--playwright-driver-url", envvar = "PLAYWRIGHT_DRIVER_URL").required()

    override fun run() {
        LOGGER.debug("Creating Google Calendar service")
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
            LOGGER.debug("Creating web driver")
            val response = createWebDriver().bind().use { pw ->
                LOGGER.debug("Connecting to browser")
                val browser = connectToBrowser(pw, playwrightDriverUrl).bind()

                LOGGER.debug("Logging into kronos")
                val page = login(browser, username, password).bind()

                LOGGER.debug("Fetching events")
                requestEventsJson(
                    page, EventsRequest(syncFrom, syncTo, EventType.ALL)
                ).bind()
            }

            JSON.tryDecodeFromString<Events>(response)
                .onFailure { LOGGER.error(response, it) }
                .bind()
        }.getOrThrow()

        LOGGER.debug("Transforming events")
        val roster = events.mapNotNull { it.toGoogleEventOrNull() }

        LOGGER.atDebug()
            .addKeyValue("syncFrom", syncFrom)
            .addKeyValue("syncTo", syncTo)
            .addKeyValue("zone", ZONE_MELBOURNE)
            .addKeyValue("domain", DOMAIN)
            .log("Synchronising to Google Calendar")

        sync(
            service, calendar,
            syncFrom..syncTo,
            roster,
            ZONE_MELBOURNE,
            DOMAIN
        )

        LOGGER.debug("Done")
    }
}