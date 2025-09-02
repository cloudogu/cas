package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Aspect
@Configuration("CesServicesManagerNullGuardAspectConfiguration")
@ComponentScan("de.triology.cas.services")
@Slf4j
public class CesServicesManagerNullGuardAspectConfiguration {

  @Around("execution(java.util.Collection org.apereo.cas.services.ServicesManager.load(..))")
  public Object aroundLoad(final ProceedingJoinPoint pjp) throws Throwable {
    Object out = pjp.proceed();
    if (out == null) {
      LOGGER.warn("ServicesManager {} returned null from load(); converting to empty list",
          pjp.getTarget().getClass().getName());
      return Collections.emptyList();
    }
    return out;
  }
}
