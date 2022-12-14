package com.tfowl.monashhealth

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal

val JSON = Json {
    ignoreUnknownKeys = true

    serializersModule = SerializersModule {
        // PayCodeEdit events don't have the 'T' for some reason, but a space
        contextual(LocalDateTimeSerialiser(DateTimeFormatter.ofPattern("yyyy-MM-dd[' ']['T']HH:mm:ss")))
        contextual(LocalDateSerialiser(DateTimeFormatter.ISO_LOCAL_DATE))
        contextual(InstantSerialiser())
    }
}

inline fun <reified T> StringFormat.tryDecodeFromString(string: String): Result<T, SerializationException> =
    com.github.michaelbull.result.runCatching {
        decodeFromString<T>(string)
    }.mapError { it as? SerializationException ?: SerializationException(it) }

class InstantSerialiser : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
}

class LocalDateTimeSerialiser(private val formatter: DateTimeFormatter) : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString(), formatter)
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.format(formatter))
}

class LocalDateSerialiser(private val formatter: DateTimeFormatter) : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.time.LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString(), formatter)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.format(formatter))
}