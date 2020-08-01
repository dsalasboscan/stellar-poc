package com.davidsalas.blockchain.common

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.client.RestTemplate
import org.stellar.sdk.Server

@Configuration
class BeansConfig {

    @Bean
    fun jacksonMapperBuilder(): Jackson2ObjectMapperBuilder {
        return Jackson2ObjectMapperBuilder().propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
    }

    @Bean
    fun getTestServer(): Server {
        return Server(Constants.HORIZON_TEST_NET_URL)
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryTemplate = RetryTemplate()
        val simpleRetryPolicy = SimpleRetryPolicy(5)
        retryTemplate.setRetryPolicy(simpleRetryPolicy)
        return retryTemplate
    }
}