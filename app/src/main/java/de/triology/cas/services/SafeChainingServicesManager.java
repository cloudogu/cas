package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;

import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.mgmt.DefaultChainingServicesManager;
import java.util.stream.Collectors;

@Slf4j
public class SafeChainingServicesManager extends DefaultChainingServicesManager {
  @Override
  public Collection<RegisteredService> load() {
    // iterate inner managers but never trust their return values
    try {    
        LOGGER.warn("[SafeChain] load() call");
    return getServiceManagers().stream()
      .map(mgr -> {
        try {
          Collection<RegisteredService> c = mgr.load();
          if (c == null) {
            LOGGER.warn("[SafeChain] {}#load() returned NULL — treating as empty",
                mgr.getClass().getName());
            return Collections.<RegisteredService>emptyList();
          }
          LOGGER.warn("[SafeChain] {}#load() -> {} service(s)",
              mgr.getClass().getName(), c.size());
            return c;
        } catch (Exception ex) {
            LOGGER.warn("[SafeChain] {}#load() threw: {}", mgr.getClass().getName(),
              ex.getMessage(), ex);
            return Collections.<RegisteredService>emptyList();
        }
      })
      .flatMap(Collection::stream)
      .collect(Collectors.toList());}
    catch (Exception ex){
          LOGGER.error("[SafeChain] load() threw: {}",
              ex.getMessage(), ex);
          return Collections.<RegisteredService>emptyList();        
    }
  }
}
