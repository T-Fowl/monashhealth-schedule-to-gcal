@file:JvmName("Main")

package com.tfowl.monashhealth.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file

fun CliktCommand.googleCalendarOption() = option("--calendar", envvar = "GOOGLE_CALENDAR_ID")

fun CliktCommand.googleClientSecretsOption() = mutuallyExclusiveOptions(
    option("--google-client-secrets").file().convert { it.reader() },
    option("--google-secrets", envvar = "GOOGLE_CLIENT_SECRETS").convert { it.reader() }
).single()

class MonashHealthCommand(): NoOpCliktCommand()

fun main(args: Array<String>) {
    MonashHealthCommand().subcommands(
        AuthCommand(),
        SyncCommand(),
        ListCommand(),
    ).main(args)
}