package de.triology.cas.services.dogu;

import junit.framework.TestCase;

public class CesDoguServiceFactoryTest extends TestCase {

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex_AllLower() {
        String fqdn = "local.cloudogu.com";
        String expected = "[Ll][Oo][Cc][Aa][Ll].[Cc][Ll][Oo][Uu][Dd][Oo][Gg][Uu].[Cc][Oo][Mm]";

        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);
        assertEquals(expected, fqdnRegex);
        assertTrue(fqdn.matches(expected));
    }

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex_AllUpper() {
        String fqdn = "SUPER.cloudogu.com";
        String expected = "[Ss][Uu][Pp][Ee][Rr].[Cc][Ll][Oo][Uu][Dd][Oo][Gg][Uu].[Cc][Oo][Mm]";

        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);
        assertEquals(expected, fqdnRegex);
        assertTrue(fqdn.matches(expected));
    }

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex_Mixed() {
        String fqdn = "SuPer.loCAl.cloUDogu.cOM";
        String expected = "[Ss][Uu][Pp][Ee][Rr].[Ll][Oo][Cc][Aa][Ll].[Cc][Ll][Oo][Uu][Dd][Oo][Gg][Uu].[Cc][Oo][Mm]";

        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);
        assertEquals(expected, fqdnRegex);
        assertTrue(fqdn.matches(expected));
    }

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex_IP() {
        String fqdn = "192.168.56.2";
        String expected = "192.168.56.2";

        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);
        assertEquals(expected, fqdnRegex);
        assertTrue(fqdn.matches(expected));
    }
}