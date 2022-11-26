package com.tfowl.monashhealth

import com.tfowl.gcal.buildSafeExtendedProperties
import com.tfowl.gcal.toGoogleEventDateTime
import kotlinx.serialization.encodeToString
import java.time.LocalTime
import java.time.ZoneId
import com.google.api.services.calendar.model.Event as GoogleEvent

const val DOMAIN = "monashhealth.tfowl.com"
const val LOCATION_CLAYTON = "Monash Medical Centre, 246 Clayton Rd, Clayton VIC 3168, Australia"
val ZONE_MELBOURNE = ZoneId.of("Australia/Melbourne")

val EXT_PROP_CREATED_BY_MARKER = "created-by" to "com.tfowl.monashhealth:monashhealth-schedule-to-gcal"
const val EXT_PROP_KEY_EVENT_JSON = "event:json"
const val EXT_PROP_KEY_TYPE = "type"
const val EXT_PROP_KEY_ID = "id"


fun Event.toGoogleEventOrNull(): GoogleEvent? {
    if (this is Event.Holiday) return null
    if (this is Event.OpenShift) return null

    val commonEvent = GoogleEvent()
        .setICalUID("$id@$DOMAIN")
        .setStart(startDateTime.toGoogleEventDateTime(ZONE_MELBOURNE))
        .setEnd(endDateTime.toGoogleEventDateTime(ZONE_MELBOURNE))
        .setSummary(title)
        .setStatus("confirmed")
        .buildSafeExtendedProperties {
            private {
                prop(EXT_PROP_KEY_TYPE to type().name)
                prop(EXT_PROP_KEY_ID to id)
                prop(EXT_PROP_CREATED_BY_MARKER)
                prop(EXT_PROP_KEY_EVENT_JSON to JSON.encodeToString(this@toGoogleEventOrNull))
            }
        }

    @Suppress("KotlinConstantConditions")
    val customisedEvent = when (this) {
        is Event.RegularShift  -> customiseRegularShift(commonEvent)
        is Event.TransferShift -> customiseTransferShift(commonEvent)
        is Event.PayCodeEdit   -> customisePayCodeEdit(commonEvent)
        is Event.OpenShift     -> error("Should not reach here!")
        is Event.Holiday       -> error("Should not reach here!")
    } ?: return null

    return customisedEvent
}

fun Event.PayCodeEdit.customisePayCodeEdit(event: GoogleEvent): GoogleEvent {
    if (title.equals("Study Leave", ignoreCase = true)) return event.setSummary("Study Leave \uD83D\uDCDA")
    return event
}

fun Event.RegularShift.customiseRegularShift(event: GoogleEvent): GoogleEvent {
    return event
        .setSummary(regularShiftSummary(this, job, commentNotes ?: emptyList()))
        .setLocation(LOCATION_CLAYTON)
}

fun Event.TransferShift.customiseTransferShift(event: GoogleEvent): GoogleEvent {
    // As far as I can tell a transfer shift is basically a regular shift
    return event
        .setSummary(regularShiftSummary(this, job, commentNotes ?: emptyList()))
        .setLocation(LOCATION_CLAYTON)
}

private fun regularShiftSummary(shift: Event, job: String, comments: List<CommentNotes>): String = buildString {
    // Starting before midday --> AM shift
    if (shift.startDateTime.toLocalTime().isBefore(LocalTime.NOON))
        append("AM \uD83C\uDF05")
    // End day is after start day --> Night shift
    else if (shift.endDateTime.toLocalDate().isAfter(shift.startDateTime.toLocalDate())) append("ND \uD83C\uDF03")
    // Otherwise, start after noon and finish on the same day --> PM shift
    else append("PM \uD83C\uDF07")

    if (job != "RN") append(" $job")

    // Example of a comment is for Supernumerary shifts
    if (comments.isNotEmpty())
        comments.joinTo(this, prefix = " [", postfix = "]") { it.comment.name }
}