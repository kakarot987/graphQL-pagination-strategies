package com.portfolio.graphql.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQlConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .type("ProductConnection",  b -> b.defaultDataFetcher(env -> null))
                .type("ActivityConnection", b -> b.defaultDataFetcher(env -> null))
                .type("UserConnection",     b -> b.defaultDataFetcher(env -> null));
    }
}