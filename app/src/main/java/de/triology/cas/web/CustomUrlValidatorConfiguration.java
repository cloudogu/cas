package de.triology.cas.web;

import org.apache.commons.validator.routines.DomainValidator;
import org.apereo.cas.web.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.apache.commons.validator.routines.UrlValidator.ALLOW_LOCAL_URLS;

@Configuration("CustomUrlValidatorConfiguration")
public class CustomUrlValidatorConfiguration {
    @Value("${custom.validation.allow-local-urls}")
    public boolean allowLocalUrls;

    @Bean
    public UrlValidator urlValidator() {
        if (allowLocalUrls) {
            return new CustomUrlValidator(
                    new org.apache.commons.validator.routines.UrlValidator(null, null, ALLOW_LOCAL_URLS, DomainValidator.getInstance(true)),
                    DomainValidator.getInstance(true)
            );
        }
        return new CustomUrlValidator(
                org.apache.commons.validator.routines.UrlValidator.getInstance(),
                DomainValidator.getInstance()
        );
    }
}
