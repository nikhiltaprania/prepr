package com.springrag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.springrag.config.MemoryProperties;
import com.springrag.security.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties({MemoryProperties.class, JwtProperties.class})
public class SpringRagApplication {

    private static final Logger log = LoggerFactory.getLogger(SpringRagApplication.class);

    public static void main(String[] args) {
        log.info("Starting spring-rag with default timezone {}", java.util.TimeZone.getDefault().getID());
        SpringApplication.run(SpringRagApplication.class, args);
    }

}
