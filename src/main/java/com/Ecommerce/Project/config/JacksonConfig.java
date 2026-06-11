package com.Ecommerce.Project.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper(){
        ObjectMapper mapper = new ObjectMapper();
        //we are registering JavaTimeModule because Jackson doesn’t know how to serialize/deserialize Java 8 date/time types like LocalDateTime
        mapper.registerModule(new JavaTimeModule());
        //Disabling WRITE_DATES_AS_TIMESTAMPS makes JSON output more human-readable (e.g., "2026-05-22T07:16:00" instead of epoch numbers).
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY,true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
        return mapper;
    }
}
