package com.definex.survey.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @Scheduled} işlerini açar (ör. süresi dolan anketlerin kapatılması). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
