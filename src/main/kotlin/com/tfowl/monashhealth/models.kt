@file:OptIn(ExperimentalSerializationApi::class)

package com.tfowl.monashhealth

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class EventsRequest(
    @Contextual val startDate: LocalDate,
    @Contextual val endDate: LocalDate,
    val types: List<EventType>,
)

@Serializable
data class EventType(val name: String) {
    companion object {
        val ALL = listOf(
            "approvedtimeoffrequest", // TODO: Haven't seen in the wild yet
            "holiday",
            "inprogresstimeoffrequest", // TODO: Haven't seen in the wild yet
            "openshift",
            "paycodeedit",
            "regularshift",
            "scheduletag", // TODO: Haven't seen in the wild yet
            "transfershift"
        ).map(::EventType)
    }
}

typealias Events = List<Event>

@Serializable
@Polymorphic
@JsonClassDiscriminator("eventType")
sealed class Event {
    abstract val id: String
    abstract val title: String

    abstract val startDateTime: LocalDateTime
    abstract val endDateTime: LocalDateTime

    @Serializable
    @SerialName("regularshift")
    data class RegularShift(
        override val id: String,
        val commentNotes: List<CommentNotes>? = null,
        override val title: String,
        val job: String,
        val location: String,
        @Contextual override val startDateTime: LocalDateTime,
        @Contextual override val endDateTime: LocalDateTime,
        val selfServiced: Boolean,
        val orderedSegments: List<OrderedSegment>,

        @SerialName("originalId") val originalID: Long,

        val position: JsonElement? = null,
        val partial: Boolean,
    ) : Event()

    @Serializable
    @SerialName("transfershift")
    data class TransferShift(
        override val id: String,
        val commentNotes: List<CommentNotes>? = null,
        override val title: String,
        val job: String,
        val location: String,
        @Contextual override val startDateTime: LocalDateTime,
        @Contextual override val endDateTime: LocalDateTime,
        val selfServiced: Boolean,
        val orderedSegments: List<OrderedSegment>,

        @SerialName("originalId")
        val originalID: Long,

        val position: JsonElement? = null,
        val partial: Boolean,
    ) : Event()

    @Serializable
    @SerialName("holiday")
    data class Holiday(
        override val id: String,
        override val title: String,
        @Contextual @SerialName("startDateTime") val startDate: LocalDate,
        @Contextual @SerialName("endDateTime") val endDate: LocalDate,
    ) : Event() {
        override val startDateTime: LocalDateTime get() = startDate.atStartOfDay()
        override val endDateTime: LocalDateTime get() = endDate.plusDays(1).atStartOfDay()
    }

    @Serializable
    @SerialName("openshift")
    data class OpenShift(
        override val id: String,
        @Contextual @SerialName("startDateTime") val startDate: LocalDate,
        @Contextual @SerialName("endDateTime") val endDate: LocalDate,
        val count: Int,
    ) : Event() {
        override val startDateTime: LocalDateTime get() = startDate.atStartOfDay()
        override val endDateTime: LocalDateTime get() = endDate.plusDays(1).atStartOfDay()
        override val title: String = "Open Shift [$count]"
    }

    @Serializable
    @SerialName("paycodeedit")
    data class PayCodeEdit(
        override val id: String,
        val commentNotes: List<CommentNotes>? = null,
        override val title: String,
        val shortTitle: String,
        @Contextual override val startDateTime: LocalDateTime,
        @Contextual @SerialName("endDateTime") val endDateTimeMaybe: LocalDateTime? = null,
        val payCodeUnit: Int,
        val currencySymbol: String,
        val position: JsonElement? = null,
        val amount: Int,
    ) : Event() {
        override val endDateTime: LocalDateTime get() = endDateTimeMaybe ?: startDateTime.plusMinutes(amount.toLong())
    }
}

fun Event.type(): EventType = EventType(this::class.java.getAnnotation(SerialName::class.java).value)

@Serializable
data class CommentNotes(val comment: Comment, val notes: JsonArray? = null)

@Serializable
data class Comment(val id: Int, val name: String)

@Serializable
data class OrderedSegment(
    @Contextual val startDateTimeUTC: LocalDateTime,
    @Contextual val endDateTimeUTC: LocalDateTime,
    val segmentType: SegmentType,
    val orgNode: OrgNode,
    val segmentTags: JsonArray,
    val transferCostCenter: Boolean,
    val costCenter: CostCenter,
    val transferLaborCategories: Boolean,
    val laborString: String,
    val transferWorkrule: Boolean,
    val workruleRef: CostCenter,
    val skillsAndCertifications: SkillsAndCertifications,
)

@Serializable
data class CostCenter(val id: Long, val qualifier: String)

@Serializable
data class OrgNode(val path: String)

@Serializable
data class SegmentType(
    val displayName: String,
    @SerialName("symbolicId") val symbolicID: String,
)

@Serializable
data class SkillsAndCertifications(
    val profiles: JsonArray,
    val skills: JsonArray,
    val certifications: JsonArray,
)
