package de.triology.cas.services.lang;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LangInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String locale = request.getLocale().getLanguage();
        if (!locale.equals("de")) {
            locale = "en";
        }
        request.setAttribute("lang", locale);
        return true;
    }
}
