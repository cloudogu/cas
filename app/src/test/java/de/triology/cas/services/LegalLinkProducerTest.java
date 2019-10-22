package de.triology.cas.services;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class LegalLinkProducerTest {

    private final String keyTos = "/config/cas/legal_urls/terms_of_service";
    private final String keyImpr = "/config/cas/legal_urls/imprint";
    private final String keyPriv = "/config/cas/legal_urls/privacy_policy";

    @Test
    public void fullOutputShouldContainThreePartsAnd2Delimiters() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getTermsOfServiceLink() + " " +
                sut.getTermsOfServiceLinkDelimiter() + " " +
                sut.getImprintLink() + " " +
                sut.getImprintLinkDelimiter() + " " +
                sut.getPrivacyPolicyLink();

        assertEquals("http://example.com/1 | http://example.com/2 | http://example.com/3", actual);
    }

    @Test
    public void fullOutputShouldContainTwoPartsAnd1Delimiters() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenThrow(new RegistryException(new RuntimeException("100: Key not found")));
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getTermsOfServiceLink() + " " +
                sut.getTermsOfServiceLinkDelimiter() + " " +
                sut.getImprintLink() + " " +
                sut.getImprintLinkDelimiter() + " " +
                sut.getPrivacyPolicyLink();

        String ignoreWhitespaceBecauseHTML = actual.trim();
        assertEquals("http://example.com/1 | http://example.com/2", ignoreWhitespaceBecauseHTML);
    }

    @Test
    public void getTermsOfServiceLinkFragment_linkWithFollowingDelimiter() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getTermsOfServiceLink();

        String expected = "http://example.com/1";
        assertEquals(expected, actual);
    }

    @Test
    public void getTermsOfServiceLinkFragment_singleLink() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getTermsOfServiceLink();

        String expected = "http://example.com";
        assertEquals(expected, actual);
    }

    @Test
    public void getTermsOfServiceLinkFragment_noLink() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getTermsOfServiceLink();

        String expected = "";
        assertEquals(expected, actual);
    }

    @Test
    public void getImprintFragment_linkWithFollowingDelimiter() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getImprintLink();

        String expected = "http://example.com/2";
        assertEquals(expected, actual);
    }

    @Test
    public void getImprintFragment_singleLink() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getImprintLink();

        String expected = "http://example.com/2";
        assertEquals(expected, actual);
    }

    @Test
    public void getImprintFragment_noLink() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getImprintLink();

        String expected = "";
        assertEquals(expected, actual);
    }

    @Test
    public void getPrivacyPolicyFragment_singleLink() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getPrivacyPolicyLink();

        String expected = "http://example.com/3";
        assertEquals(expected, actual);
    }

    @Test
    public void getPrivacyPolicyFragment_noLink() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getPrivacyPolicyLink();

        String expected = "";
        assertEquals(expected, actual);
    }

    @Test
    public void getTermsOfServiceLinkDelimiterShouldReturnDelimiter() {
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        String actual = sut.getTermsOfServiceLinkDelimiter();

        assertEquals("|", actual);
    }

    @Test
    public void FirstGetTermsOfServiceLinkShouldNotHitTheCache() {
        // given
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        assertNull(sut.etcdKeyToValueCache.get(keyTos));

        // when
        String actual = sut.getTermsOfServiceLink();

        // then
        verify(mockRegistry, times(1)).getEtcdValueForKey(keyTos);
        String expected = "http://example.com/1";
        assertEquals(sut.etcdKeyToValueCache.get(keyTos), expected);
        assertEquals(expected, actual);
    }

    @Test
    public void SecondGetTermsOfServiceLinkShouldHitTheCache() {
        // given
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        // when
        String actual = sut.getTermsOfServiceLink();
        String actual2 = sut.getTermsOfServiceLink();

        // then
        verify(mockRegistry, times(1)).getEtcdValueForKey(keyTos);
        String expected = "http://example.com/1";
        assertEquals(sut.etcdKeyToValueCache.get(keyTos), expected);
        assertEquals(expected, actual);
        assertEquals(expected, actual2);
    }

    @Test
    public void FirstGetImprintLinkShouldNotHitTheCache() {
        // given
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        assertNull(sut.etcdKeyToValueCache.get(keyImpr));

        // when
        String actual = sut.getImprintLink();

        // then
        verify(mockRegistry, times(1)).getEtcdValueForKey(keyImpr);
        String expected = "http://example.com/2";
        assertEquals(sut.etcdKeyToValueCache.get(keyImpr), expected);
        assertEquals(expected, actual);
    }

    @Test
    public void SecondGetImprintLinkLinkShouldHitTheCache() {
        // given
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        // when
        String actual = sut.getImprintLink();
        String actual2 = sut.getImprintLink();

        // then
        verify(mockRegistry, times(1)).getEtcdValueForKey(keyImpr);
        String expected = "http://example.com/2";
        assertEquals(sut.etcdKeyToValueCache.get(keyImpr), expected);
        assertEquals(expected, actual);
        assertEquals(expected, actual2);
    }

    @Test
    public void FirstGetPrivacyPolicyLinkShouldNotHitTheCache() {
        // given
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        assertNull(sut.etcdKeyToValueCache.get(keyPriv));

        // when
        String actual = sut.getPrivacyPolicyLink();

        // then
        verify(mockRegistry, times(1)).getEtcdValueForKey(keyPriv);
        String expected = "http://example.com/3";
        assertEquals(sut.etcdKeyToValueCache.get(keyPriv), expected);
        assertEquals(expected, actual);
    }

    @Test
    public void SecondGetPrivacyPolicyLinkShouldHitTheCache() {
        // given
        RegistryEtcd mockRegistry = Mockito.mock(RegistryEtcd.class);
        when(mockRegistry.getEtcdValueForKey(keyTos)).thenReturn("http://example.com/1");
        when(mockRegistry.getEtcdValueForKey(keyImpr)).thenReturn("http://example.com/2");
        when(mockRegistry.getEtcdValueForKey(keyPriv)).thenReturn("http://example.com/3");
        LegalLinkProducer sut = new LegalLinkProducer(mockRegistry);

        // when
        String actual = sut.getPrivacyPolicyLink();
        String actual2 = sut.getPrivacyPolicyLink();

        // then
        verify(mockRegistry, times(1)).getEtcdValueForKey(keyPriv);
        String expected = "http://example.com/3";
        assertEquals(sut.etcdKeyToValueCache.get(keyPriv), expected);
        assertEquals(expected, actual);
        assertEquals(expected, actual2);
    }
}