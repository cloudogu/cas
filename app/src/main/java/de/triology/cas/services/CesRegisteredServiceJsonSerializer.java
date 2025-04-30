package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.services.util.RegisteredServiceJsonSerializer;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class CesRegisteredServiceJsonSerializer extends RegisteredServiceJsonSerializer {

    public CesRegisteredServiceJsonSerializer(ConfigurableApplicationContext context) {
        super(context);
    }
}
