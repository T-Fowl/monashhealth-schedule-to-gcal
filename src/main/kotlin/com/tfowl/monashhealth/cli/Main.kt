@file:JvmName("Main")

package com.tfowl.monashhealth.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class MonashHealthCommand(): NoOpCliktCommand()

fun main(args: Array<String>) {
    MonashHealthCommand().subcommands(
        SyncCommand()
    ).main(args)
}