package de.triology.cas.web;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.UrlValidator;

@RequiredArgsConstructor
public class CustomUrlValidator implements org.apereo.cas.web.UrlValidator {

    private final UrlValidator urlValidator;

    private final DomainValidator domainValidator;

    @Override
    public boolean isValid(final String value) {
        return StringUtils.isNotBlank(value) && this.urlValidator.isValid(value);
    }

    @Override
    public boolean isValidDomain(final String value) {
        return StringUtils.isNotBlank(value) && this.domainValidator.isValid(value);
    }
}