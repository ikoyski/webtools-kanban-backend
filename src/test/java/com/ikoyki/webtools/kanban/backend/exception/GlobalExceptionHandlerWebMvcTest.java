package com.ikoyki.webtools.kanban.backend.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerWebMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mapsBadRequestDomainExceptionTo400() throws Exception {
        mockMvc.perform(get("/test/bad-request")).andExpect(status().isBadRequest());
    }

    @Test
    void mapsForbiddenDomainExceptionTo403() throws Exception {
        mockMvc.perform(get("/test/forbidden")).andExpect(status().isForbidden());
    }

    @Test
    void mapsMissingIdentityTo401() throws Exception {
        mockMvc.perform(get("/test/unauthorized")).andExpect(status().isUnauthorized());
    }

    @Test
    void keepsUnexpectedRuntimeExceptionAt500() throws Exception {
        mockMvc.perform(get("/test/unexpected")).andExpect(status().isInternalServerError());
    }

    @RestController
    public static class TestController {
        @GetMapping("/test/bad-request")
        public void badRequest() {
            throw new BadRequestException("Invalid request");
        }

        @GetMapping("/test/forbidden")
        public void forbidden() {
            throw new ForbiddenBoardAccessException("Not a board member");
        }

        @GetMapping("/test/unauthorized")
        public void unauthorized() {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing header");
        }

        @GetMapping("/test/unexpected")
        public void unexpected() {
            throw new IllegalStateException("Unexpected failure");
        }
    }
}
