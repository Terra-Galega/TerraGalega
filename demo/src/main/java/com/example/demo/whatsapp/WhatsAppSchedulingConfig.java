package com.example.demo.whatsapp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class WhatsAppSchedulingConfig {
    // Conserva un scheduler por defecto para las tareas de pagos y reservas.
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("terra-scheduled-");
        return scheduler;
    }

    @Bean(name = "whatsappReplyScheduler")
    public ThreadPoolTaskScheduler whatsappReplyScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("whatsapp-replies-");
        return scheduler;
    }
}
