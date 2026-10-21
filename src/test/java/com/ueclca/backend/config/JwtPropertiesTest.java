package com.ueclca.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class JwtPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "app.security.jwt.issuer=back-kindergarten-test",
                    "app.security.jwt.expiration-minutes=60");

    @Test
    void failsWhenSecretIsMissing() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsWhenSecretIsTooShort() {
        contextRunner.withPropertyValues("app.security.jwt.secret=corta")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void bindsValidProperties() {
        contextRunner.withPropertyValues("app.security.jwt.secret=" + "x".repeat(32))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .getBean(JwtProperties.class)
                        .extracting(JwtProperties::issuer)
                        .isEqualTo("back-kindergarten-test"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class PropertiesConfiguration {

        @Bean
        static LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }
}
