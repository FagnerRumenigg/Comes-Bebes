package org.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class DateTimeConfig {

    @Bean
    public ZoneId applicationZoneId(@Value("${app.time-zone}") String timeZone) {
        return ZoneId.of(timeZone);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
