package xyz.crearts.note.keeper.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final ValueSerializer<LocalDateTime> UTC_LOCAL_DATETIME_SERIALIZER =
            new ValueSerializer<>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
                    gen.writeString(value.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
                }
            };

    private static final ValueSerializer<LocalDate> ISO_LOCAL_DATE_SERIALIZER =
            new ValueSerializer<>() {
                @Override
                public void serialize(LocalDate value, JsonGenerator gen, SerializationContext ctxt) {
                    gen.writeString(value.format(DateTimeFormatter.ISO_DATE));
                }
            };

    @Bean
    public JsonMapperBuilderCustomizer noteKeeperJacksonCustomizer() {
        SimpleModule timeModule = new SimpleModule("NoteKeeperTime");
        timeModule.addSerializer(LocalDateTime.class, UTC_LOCAL_DATETIME_SERIALIZER);
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        timeModule.addSerializer(LocalDate.class, ISO_LOCAL_DATE_SERIALIZER);
        return builder -> builder.addModule(timeModule);
    }
}
