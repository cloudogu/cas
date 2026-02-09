package de.triology.cas.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomUrlValidatorConfigurationTest {

    @Test
    void urlValidatorAllowLocalUrls() {
        var config = new CustomUrlValidatorConfiguration();
        config.allowLocalUrls = true;

        var urlValidator = config.urlValidator();

        var result = urlValidator.isValid("http://localhost:8080/");
        assertTrue(result);

        result = urlValidator.isValid("http://k3ces.localhost/");
        assertTrue(result);

        result = urlValidator.isValid("http://k3ces.localdomain/");
        assertTrue(result);

        result = urlValidator.isValid("http://test.com/");
        assertTrue(result);

        result = urlValidator.isValid("http://test.de/");
        assertTrue(result);
    }

    @Test
    void urlValidatorNotAllowLocalUrls() {
        var config = new CustomUrlValidatorConfiguration();
        config.allowLocalUrls = false;

        var urlValidator = config.urlValidator();

        var result = urlValidator.isValid("http://localhost:8080/");
        assertFalse(result);

        result = urlValidator.isValid("http://k3ces.localhost/");
        assertFalse(result);

        result = urlValidator.isValid("http://k3ces.localdomain/");
        assertFalse(result);

        result = urlValidator.isValid("http://test.com/");
        assertTrue(result);

        result = urlValidator.isValid("http://test.de/");
        assertTrue(result);
    }

    @Test
    void domainValidator() {
        var config = new CustomUrlValidatorConfiguration();
        config.allowLocalUrls = true;

        var urlValidator = config.urlValidator();

        var result = urlValidator.isValidDomain("localhost");
        assertTrue(result);

        result = urlValidator.isValidDomain("localdomain");
        assertTrue(result);

        result = urlValidator.isValidDomain("test");
        assertTrue(result);
    }
}