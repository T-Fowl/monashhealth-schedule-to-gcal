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

fun CliktCommand.googleCalendarOption() = option("--calendar", envvar = "MH_GCAL_ID")

fun CliktCommand.googleClientSecretsOption() = mutuallyExclusiveOptions(
    option("--google-client-secrets").file().convert { it.reader() },
    option("--google-secrets", envvar = "MG_GOOG_SECRETS").convert { it.reader() }
).single()

class MonashHealthCommand(): NoOpCliktCommand()

fun main(args: Array<String>) {
    MonashHealthCommand().subcommands(
        SyncCommand()
    ).main(args)
}