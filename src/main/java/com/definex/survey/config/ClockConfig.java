package com.definex.survey.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Zamana bağlı kuralların (oturum süresi, kilit) test edilebilmesi için tek saat kaynağı. */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
