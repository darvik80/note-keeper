package xyz.crearts.note.keeper.config;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom deserializer for LocalDateTime that handles multiple date formats.
 * Supports:
 * - ISO-8601 with 'Z' suffix (e.g., "2026-04-17T12:25:00Z") — converted from UTC
 * - ISO-8601 local format (e.g., "2026-04-17T16:25:00") — used as-is
 * - JavaScript Date format (e.g., "Fri Apr 17 2026 16:25:00 GMT+0700 (Indochina Time)")
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
            return LocalDateTime.parse(dateStr, ISO_FORMATTER);
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
