package com.lingua.learning.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

    /**
     * Injected wherever the current time matters (question timers), so that tests can control it.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
