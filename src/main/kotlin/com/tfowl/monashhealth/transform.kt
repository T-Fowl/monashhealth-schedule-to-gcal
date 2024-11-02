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
        is Event.RegularShift             -> customiseRegularShift(commonEvent)
        is Event.TransferShift            -> customiseTransferShift(commonEvent)
        is Event.PayCodeEdit              -> customisePayCodeEdit(commonEvent)
        is Event.InProgressTimeOffRequest -> customiseInProgressTimeOffRequest(commonEvent)
        is Event.ApprovedTimeOffRequest   -> customiseApprovedTimeOffRequest(commonEvent)
        is Event.OpenShift                -> error("Should not reach here!")
        is Event.Holiday                  -> error("Should not reach here!")
    } ?: return null

    return customisedEvent
}

fun Event.PayCodeEdit.customisePayCodeEdit(event: GoogleEvent): GoogleEvent {
    if (title.startsWith("SICK LVE", ignoreCase = true) || title.startsWith("SICK LEAVE", ignoreCase = true))
        return event.setSummary("\uD83E\uDD12 ${event.summary}")
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

fun Event.InProgressTimeOffRequest.customiseInProgressTimeOffRequest(event: GoogleEvent): GoogleEvent {
    return event
        .setSummary("$title ($localizedRequestTitle) [${this.currentStateLabel}]")
}

fun Event.ApprovedTimeOffRequest.customiseApprovedTimeOffRequest(event: GoogleEvent): GoogleEvent {
    return event
        .setSummary("$title ($localizedRequestTitle) [${this.currentStateLabel}]")
}

private fun regularShiftSummary(shift: Event, job: String, comments: List<CommentNotes>): String = buildString {
    val isFlex = "FLEX" in job
    val startTime = shift.startDateTime.toLocalTime()
    val startDate = shift.startDateTime.toLocalDate()
    val finishDate = shift.endDateTime.toLocalDate()

    val hub = Regex("""\d+ (?<hub>H\d+)""").let { regex ->
        regex.find(shift.title)?.let { mr ->
            mr.groups["hub"]?.value
        }
    }?.let { " ($it)" } ?: ""

    if (startTime < LocalTime.NOON)
        if (isFlex)
            append("AM FLEX$hub \uD83C\uDF05")
        else
            append("AM$hub \uD83C\uDF05")
    else if (startDate < finishDate)
        if (isFlex)
            append("PM FLEX$hub \uD83C\uDF03")
        else
            append("ND$hub \uD83C\uDF03")
    else
        append("PM$hub \uD83C\uDF07")

    append(" ${shift.title}")

    if (job != "RN" && job != "RN-FLEX17" && job != "RN-FLEX9") append(" $job")

    // Example of a comment is for Supernumerary shifts
    if (comments.isNotEmpty())
        comments.joinTo(this, prefix = " [", postfix = "]") { it.comment.name }
}