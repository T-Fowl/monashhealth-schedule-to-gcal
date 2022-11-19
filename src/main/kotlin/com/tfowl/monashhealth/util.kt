package com.tfowl.monashhealth

import java.time.LocalDate

fun String.toLocalDateOrNull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (ignored: Throwable) {
    null
}