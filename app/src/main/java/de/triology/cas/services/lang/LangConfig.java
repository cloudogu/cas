package de.triology.cas.services.lang;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.util.Locale;

@Configuration("LangConfig")
public class LangConfig implements WebMvcConfigurer {

    private LangInterceptor langInterceptor;

    @Autowired
    public LangConfig(LangInterceptor langInterceptor) {
        super();
    }

    @Bean
    public CookieLocaleResolver langResolver() {
        CookieLocaleResolver localeResolver = new CookieLocaleResolver("lang");
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(langInterceptor);
    }


}
