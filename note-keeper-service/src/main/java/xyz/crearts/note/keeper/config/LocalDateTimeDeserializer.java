package xyz.crearts.note.keeper.config;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom deserializer for LocalDateTime that handles multiple date formats.
 * All values are normalized to UTC for consistent storage:
 * - ISO-8601 with 'Z' suffix (e.g., "2026-04-17T12:25:00Z") — already UTC
 * - ISO-8601 local format (e.g., "2026-04-17T16:25:00") — treated as system local, converted to UTC
 * - JavaScript Date format (e.g., "Fri Apr 17 2026 16:25:00 GMT+0700 (Indochina Time)") — converted to UTC
 */
public class LocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter JS_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (z)");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
        String dateStr = p.getString().trim();

        if (dateStr.isEmpty()) {
            return null;
        }

        if (dateStr.endsWith("Z")) {
            try {
                java.time.Instant instant = java.time.Instant.parse(dateStr);
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                // Fall through to other formats
            }
        }

        try {
            // Local datetime without timezone — treat as system local, convert to UTC
            return LocalDateTime.parse(dateStr, ISO_FORMATTER)
                    .atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr, JS_DATE_FORMATTER);
                return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            } catch (DateTimeParseException e2) {
                try {
                    DateTimeFormatter simpleFormatter = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss 'GMT'Z");
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr, simpleFormatter);
                    return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Unable to parse date: " + dateStr, e3);
                }
            }
        }
    }
}
