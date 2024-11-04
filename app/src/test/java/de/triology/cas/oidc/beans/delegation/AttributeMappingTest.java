package de.triology.cas.oidc.beans.delegation;

import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AttributeMappingTest {

    @Test
    public void testFromPropertyString() {
        String propertyString = "family_name:surname,given_name:givenName,email:mail";

        List<AttributeMapping> mappings = AttributeMapping.fromPropertyString(propertyString);

        assertEquals(3, mappings.size());
        assertEquals("family_name", mappings.get(0).getSource());
        assertEquals("surname", mappings.get(0).getTarget());
        assertEquals("given_name", mappings.get(1).getSource());
        assertEquals("givenName", mappings.get(1).getTarget());
        assertEquals("email", mappings.get(2).getSource());
        assertEquals("mail", mappings.get(2).getTarget());
    }

    @Test
    public void testFromPropertyString_withEmptyString() {
        String prpertyString = "";

        List<AttributeMapping> mappings = AttributeMapping.fromPropertyString(prpertyString);

        assertEquals(0, mappings.size());
    }
}