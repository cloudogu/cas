package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Collections;

@Aspect
@Configuration("CesServicesManagerNullGuardAspectConfiguration")
@EnableAspectJAutoProxy(proxyTargetClass = false)
@Slf4j
public class CesServicesManagerNullGuardAspectConfiguration {
  
  /** Normalize null results from ServicesManager.load(..) while preserving thrown failures. */
  @Around("execution(java.util.Collection *..ServicesManager+.load(..))")
  public Object guardServicesManagerLoad(final ProceedingJoinPoint pjp) throws Throwable {
    return normalizeNullResult(pjp);
  }

  /** Normalize null results from ServiceRegistry.load(..) while preserving thrown failures. */
  @Around("execution(java.util.Collection *..ServiceRegistry+.load(..))")
  public Object guardServiceRegistryLoad(final ProceedingJoinPoint pjp) throws Throwable {
    return normalizeNullResult(pjp);
  }

  private Object normalizeNullResult(final ProceedingJoinPoint pjp) throws Throwable {
    Object out = pjp.proceed();
    if (out == null) {
      LOGGER.debug("Guard: {}.load(..) returned null → using empty list", pjp.getTarget().getClass().getName());
      return Collections.emptyList();
    }
    return out;
  }
}
