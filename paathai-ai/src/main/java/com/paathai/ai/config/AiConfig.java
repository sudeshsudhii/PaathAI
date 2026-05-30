package com.paathai.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * AI module configuration.
 * Provides Gemini API properties and a configured RestTemplate.
 */
@Configuration
public class AiConfig {

    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 10 seconds
        factory.setReadTimeout(60_000);     // 60 seconds (LLM calls can be slow)
        return new RestTemplate(factory);
    }
}
