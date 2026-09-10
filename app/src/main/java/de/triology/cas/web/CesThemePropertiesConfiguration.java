package de.triology.cas.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * Exposes {@code cas-theme-default.properties} as a plain Spring bean for templates.
 * <p>
 * Thymeleaf's {@code #themes} expression object relies on Spring's classic ThemeResolver/Theme
 * API, which Spring Framework 7 removed entirely; thymeleaf-spring6 (no thymeleaf-spring7
 * exists yet) still calls the now-missing {@code RequestContext.getTheme()}, throwing
 * {@code NoSuchMethodError} on every template render. This repo never uses per-service dynamic
 * theme switching (a single properties file, always "default"), so the properties are loaded
 * once here and templates read them via {@code @themeProperties.getProperty('key')} instead.
 */
@Configuration("CesThemePropertiesConfiguration")
public class CesThemePropertiesConfiguration {

    @Bean
    public Properties themeProperties() throws IOException {
        return PropertiesLoaderUtils.loadProperties(new ClassPathResource("cas-theme-default.properties"));
    }
}
