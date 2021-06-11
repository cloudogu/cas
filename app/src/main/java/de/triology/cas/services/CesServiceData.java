package de.triology.cas.services;

import java.util.HashMap;
import java.util.Map;

public class CesServiceData {
    private String name;
    private ICesServiceFactory factory;
    private Map<String, String> attributes;

    public CesServiceData(String name, ICesServiceFactory factory, Map<String, String> attributes) {
        this.name = name;
        this.factory = factory;
        this.attributes = attributes;
    }

    public CesServiceData(String name, ICesServiceFactory factory) {
        this.name = name;
        this.factory = factory;
        this.attributes = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public ICesServiceFactory getFactory() {
        return this.factory;
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    public String getIdentifier() {
        return factory.getClass().getSimpleName() + " " + name;
    }
}
