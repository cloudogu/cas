package de.triology.cas.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CesThemePropertiesConfigurationTest {

    @Test
    void themePropertiesLoadsCasThemeDefaultProperties() throws IOException {
        var config = new CesThemePropertiesConfiguration();

        var themeProperties = config.themeProperties();

        assertEquals("/css/cloudogu-cas.css", themeProperties.getProperty("cas.standard.css.file"));
        assertEquals("/css/ces-theme-tailwind.css", themeProperties.getProperty("cas.theme.css.file"));
        assertEquals("/js/cas.js,/js/material.js", themeProperties.getProperty("cas.standard.js.file"));
    }
}
