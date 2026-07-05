package xyz.crearts.note.keeper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    /**
     * Serializes LocalDateTime as UTC ISO instant string (with 'Z' suffix).
     * This ensures the frontend always knows the value is UTC and can
     * correctly convert to the user's local timezone via `new Date(str)`.
     */
    private static final LocalDateTimeSerializer UTC_SERIALIZER = new LocalDateTimeSerializer(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    ) {
        @Override
        protected DateTimeFormatter _defaultFormatter() {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        }

        @Override
        public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
            // Write LocalDateTime as UTC ISO instant string with 'Z' suffix
            gen.writeString(value.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        }
    };

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();

        // Serialize LocalDateTime as UTC with 'Z' suffix
        module.addSerializer(LocalDateTime.class, UTC_SERIALIZER);
        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_DATE));

        // Deserialize LocalDateTime from both 'Z'-suffixed UTC and plain local formats
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());

        mapper.registerModule(module);
        return mapper;
    }
}
