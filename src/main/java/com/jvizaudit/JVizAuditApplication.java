package com.jvizaudit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@EnableAsync
@Configuration
public class JVizAuditApplication {

    public static void main(String[] args) {
        SpringApplication.run(JVizAuditApplication.class, args);
    }

}