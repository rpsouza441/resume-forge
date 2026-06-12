package com.resumeforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Resume Forge - AI-powered resume builder application.
 *
 * <p>Main entry point for the Spring Boot application.
 * Configures JPA auditing and auto-configuration.
 */
@SpringBootApplication
@EnableJpaAuditing
public class ResumeForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeForgeApplication.class, args);
    }
}
