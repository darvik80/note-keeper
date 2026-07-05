package xyz.crearts.note.keeper.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    /**
     * Serializes LocalDateTime as UTC ISO instant string with 'Z' suffix.
     * This ensures the frontend always knows the value is UTC and can
     * correctly convert to the user's local timezone via {@code new Date(str)}.
     */
    private static final StdSerializer<LocalDateTime> UTC_LOCAL_DATETIME_SERIALIZER = new StdSerializer<>(LocalDateTime.class) {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen,
                              SerializerProvider provider) throws IOException {
            gen.writeString(value.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        }
    };

    /**
     * Customizes the Jackson ObjectMapper used by Spring MVC HTTP message converters.
     * Uses Spring Boot's customizer mechanism to ensure it applies to the auto-configured ObjectMapper.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                // Serialize LocalDateTime as UTC with 'Z' suffix
                .serializerByType(LocalDateTime.class, UTC_LOCAL_DATETIME_SERIALIZER)
                // Serialize LocalDate as ISO date
                .serializerByType(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_DATE))
                // Deserialize LocalDateTime from both 'Z'-suffixed UTC and plain local formats
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer())
                // Deserialize LocalDate from ISO date format
                .deserializerByType(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_DATE));
    }
}
