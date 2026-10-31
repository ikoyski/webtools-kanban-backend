package com.ikoyki.webtools.kanban.backend.security;

import com.ikoyki.webtools.kanban.backend.service.UserProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserProvisioningService userProvisioningService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class) &&
                parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Request not found");
        }

        String userIdHeader = request.getHeader("X-User-Id");
        String userEmailHeader = request.getHeader("X-User-Email");
        String displayNameHeader = request.getHeader("X-User-Display-Name");
        String avatarUrlHeader = request.getHeader("X-User-Avatar-Url");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            log.warn("Missing X-User-Id header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing header");
        }

        try {
            UUID userId = UUID.fromString(userIdHeader);

            // X-User-* headers are replaced by the trusted API gateway after JWT validation.
            // Provision only the Kanban-local profile; authentication and provider identity live centrally.
            userProvisioningService.ensureLocalUser(userId, userEmailHeader, displayNameHeader, avatarUrlHeader);
            log.debug("Resolved central identity {} ({}) to a Kanban-local profile", userId, userEmailHeader);
            return userId;
        } catch (IllegalArgumentException e) {
            log.warn("Malformed X-User-Id header: {}", userIdHeader);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Malformed header");
        }
    }
}
