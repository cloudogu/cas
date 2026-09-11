package de.triology.cas.pat.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import de.triology.cas.pat.model.CreatePATRequest;
import de.triology.cas.pat.model.CreatePATResponse;
import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.service.PATService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class PATControllerTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

    @Mock
    private PATService service;
    @Mock
    private Principal principal;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void createReturnsLocationAndPreventsSecretCaching() {
        UUID id = UUID.randomUUID();
        CreatePATRequest createRequest = new CreatePATRequest("name", null, null);
        CreatePATResponse serviceResponse = new CreatePATResponse(
                id, "user", "name", "pat_secret", NOW, null, "/*");
        when(principal.getName()).thenReturn("usermgt");
        when(service.create("user", createRequest, "usermgt")).thenReturn(serviceResponse);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/api/users/user/pats");
        servletRequest.setScheme("https");
        servletRequest.setServerName("cas.example.test");
        servletRequest.setServerPort(443);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        var response = new PATController(service).create("user", createRequest, principal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("https://cas.example.test/api/users/user/pats/" + id,
                response.getHeaders().getLocation().toString());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("no-cache", response.getHeaders().getFirst(HttpHeaders.PRAGMA));
        assertEquals(serviceResponse, response.getBody());
    }

    @Test
    void delegatesReadAndDeleteOperations() {
        UUID id = UUID.randomUUID();
        PATMetadata metadata = new PATMetadata(id, "user", "name", NOW, null, "/*");
        when(service.findAll("user")).thenReturn(List.of(metadata));
        when(service.findOne("user", id)).thenReturn(metadata);
        when(principal.getName()).thenReturn("usermgt");
        PATController controller = new PATController(service);

        assertEquals(List.of(metadata), controller.findAll("user"));
        assertEquals(metadata, controller.findOne("user", id));
        var deleteResponse = controller.delete("user", id, principal);

        verify(service).delete("user", id, "usermgt");
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        assertNull(deleteResponse.getBody());
    }
}
