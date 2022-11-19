@file:UseSerializers(serializerClasses = [LocalDateSerialiser::class])

package com.tfowl.monashhealth

import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import com.tfowl.gcal.GoogleApiServiceConfig
import com.tfowl.gcal.GoogleCalendar
import com.tfowl.gcal.calendarView
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import java.io.File

fun main() {
    val event = JSON.decodeFromString<Event>(
        """{
    "id": "regularshift-5669326",
    "eventType": "regularshift",
    "commentNotes": [],
    "title": "13",
    "job": "RN",
    "location": "B0005-C - Emergency Nursing",
    "startDateTime": "2022-12-01T13:00:00",
    "endDateTime": "2022-12-01T21:30:00",
    "selfServiced": false,
    "orderedSegments": [
      {
        "startDateTimeUTC": "2022-12-01T13:00:00",
        "endDateTimeUTC": "2022-12-01T21:30:00",
        "segmentType": {
          "displayName": "",
          "symbolicId": "regular_segment"
        },
        "orgNode": {
          "path": "MH/SHS01/EXE/AGA/CLA/B0005-C - Emergency Nursing/RN"
        },
        "segmentTags": [],
        "transferCostCenter": false,
        "costCenter": {
          "id": 225,
          "qualifier": "B0005"
        },
        "transferLaborCategories": false,
        "laborString": ",YP3,",
        "transferWorkrule": false,
        "workruleRef": {
          "id": 695,
          "qualifier": "22-PT-SHFT-30"
        },
        "skillsAndCertifications": {
          "profiles": [],
          "skills": [],
          "certifications": []
        }
      }
    ],
    "originalId": 5669326,
    "position": null,
    "partial": false
  }"""
    ) as Event.RegularShift

    val gEvent = event.toGoogleEventOrNull()!!

    val service = GoogleCalendar.create(
        GoogleApiServiceConfig(
            secrets = File("client-secrets.json"),
            applicationName = "APPLICATION_NAME_PLACEHOLDER",
            scopes = listOf(CalendarScopes.CALENDAR),
            dataStoreFactory = FileDataStoreFactory(File(".monashhealth-schedule"))
        )
    )

    val calendar =
        service.calendarView("17f7b62ff7b90f55b61b6e6eb7b0aa32d311b1b11cb50234e06cce0feb140cca@group.calendar.google.com")

    calendar.list()
        .setICalUID(gEvent.iCalUID)
        .setShowDeleted(true)
        .execute().items.forEach { println(it) }

//    val gEvent = calendar.get("_e9imetbcc5p76q39cpq2qd1g6srj8chi81mmurj1edk6gpb1dhq6gbjkcpnner1ecdnmq").execute()
//
//    println(gEvent.extendedProperties.private.getValue(PROP_KEY_CREATED_BY))
//    println(gEvent.extendedProperties.private.getValue(PROP_KEY_VERSION))
//    println(gEvent.extendedProperties.private.getValue(PROP_KEY_EVENT_JSON))
//
//    val event = JSON.decodeFromString<Event>(
//        gEvent.extendedProperties.private.getValue(PROP_KEY_EVENT_JSON)
//    )
//
//    println(gEvent)
//    println(event)
}

