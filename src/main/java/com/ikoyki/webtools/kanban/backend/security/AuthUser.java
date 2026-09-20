package com.ikoyki.webtools.kanban.backend.security;

import java.lang.annotation.*;

/**
 * Marks a method parameter that should be populated with the current user's ID
 * resolved from the X-User-Id gateway header.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
}
