package com.anshul.devtoolkitservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ComponentScan(basePackages = {"com.anshul.devtoolkitservice", "com.anshul.devtoolkit"})
@EnableJpaRepositories(basePackages = "com.anshul.devtoolkit.repository")
@EntityScan(basePackages = "com.anshul.devtoolkit.entity")
public class DevToolkitApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevToolkitApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(@Value("${rate-limiter.timeout-ms:200}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
