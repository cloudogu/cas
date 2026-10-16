package de.triology.cas.pat.controller;

import java.util.List;
import java.util.Map;
import de.triology.cas.pat.service.PATService;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PATPrincipalControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // Scope matching is pure logic; use the real service to verify its semantics at the HTTP boundary.
        mvc = MockMvcBuilders.standaloneSetup(new PATPrincipalController(new PATService(null, null, null)))
                .build();
    }

    @ParameterizedTest
    @CsvSource({"/redmine,/redmine", "/redmine,/redmine/api", "/*,/usermgt",
            "'/redmine, /usermgt',/usermgt/api"})
    void acceptsScopesCoveredByThePat(String storedScope, String requestedScope) throws Throwable {
        var result = mvc.perform(request(storedScope).param("scope", requestedScope))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();
        var body = JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
        assertEquals("alice", body.get("id").asText());
        assertEquals(storedScope, body.get("attributes").get("patScope").get(0).asText());
    }

    @ParameterizedTest
    @CsvSource({"/redmine,/usermgt", "/redmine,/redmine-admin", "/redmine,/*",
            "/redmine/api,/redmine", "'/redmine, /usermgt',/other"})
    void rejectsScopesNotCoveredByThePat(String storedScope, String requestedScope) throws Throwable {
        mvc.perform(request(storedScope).param("scope", requestedScope))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresNonBlankScopeEvenForUnrestrictedPat() throws Throwable {
        mvc.perform(request("/*")).andExpect(status().isBadRequest());
        mvc.perform(request("/*").param("scope", "")).andExpect(status().isBadRequest());
        mvc.perform(request("/*").param("scope", "   ")).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPrincipalWithoutScope() throws Throwable {
        mvc.perform(request(null).param("scope", "/redmine"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder request(String storedScope) throws Throwable {
        Map<String, List<Object>> attributes = storedScope == null ? Map.of()
                : Map.of(PATService.PAT_SCOPE_ATTRIBUTE, List.of(storedScope));
        var principal = PrincipalFactoryUtils.newPrincipalFactory().createPrincipal("alice", attributes);
        return get("/api/pats/validate").principal(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }
}
