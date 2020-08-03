package com.davidsalas.blockchain.common

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.stellar.sdk.Server
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger.web.UiConfiguration
import springfox.documentation.swagger.web.UiConfigurationBuilder
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
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

    @Bean
    fun api(apiInfo: ApiInfo): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController::class.java))
                .paths(PathSelectors.any())
                .build()
    }

    @Bean
    fun uiConfig() : UiConfiguration {
        return UiConfigurationBuilder.builder()
                .defaultModelsExpandDepth(-1)
                .build()
    }

    @Bean
    fun apiInfo(): ApiInfo {
        return ApiInfoBuilder()
                .title("POC Integración Stellar Network (Blockchain)")
                .description("<b> Entornos </b> \n" +
                        "- Local ROOT: http://localhost:9090 (configurable en application.yml) \n" +
                        "- Local SWAGGER-UI: http://localhost:9090/swagger-ui.html \n" +
                        "\n" +
                        "- Heroku ROOT: https://patacoin-poc.herokuapp.com \n" +
                        "- Heroku SWAGGER-UI: https://patacoin-poc.herokuapp.com//swagger-ui.html \n")

                .license("")
                .version("0.0.1")
                .build()
    }
}