package com.ikoyki.webtools.kanban.backend.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Card descriptions and comment content now carry rich-text HTML from the frontend's WYSIWYG
 * editor. These bounds (CreateCardRequest/UpdateCardRequest#description, CommentRequest#content)
 * exist purely to reject pathologically large payloads — the underlying DB columns are
 * unbounded TEXT, so nothing else enforces a limit. See the backend CLAUDE.md "Rich Text"
 * section before changing these.
 */
class CardContentSizeValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void createCardRequest_descriptionAtLimit_isValid() {
        CreateCardRequest request = CreateCardRequest.builder()
                .columnId(UUID.randomUUID())
                .title("Title")
                .description("a".repeat(50_000))
                .build();

        Set<ConstraintViolation<CreateCardRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void createCardRequest_descriptionOverLimit_isRejected() {
        CreateCardRequest request = CreateCardRequest.builder()
                .columnId(UUID.randomUUID())
                .title("Title")
                .description("a".repeat(50_001))
                .build();

        Set<ConstraintViolation<CreateCardRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("description", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void createCardRequest_noDescription_isValid() {
        CreateCardRequest request = CreateCardRequest.builder()
                .columnId(UUID.randomUUID())
                .title("Title")
                .build();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void updateCardRequest_descriptionOverLimit_isRejected() {
        UpdateCardRequest request = UpdateCardRequest.builder()
                .description("a".repeat(50_001))
                .build();

        Set<ConstraintViolation<UpdateCardRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("description", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void commentRequest_contentAtLimit_isValid() {
        CommentRequest request = CommentRequest.builder().content("a".repeat(10_000)).build();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void commentRequest_contentOverLimit_isRejected() {
        CommentRequest request = CommentRequest.builder().content("a".repeat(10_001)).build();

        Set<ConstraintViolation<CommentRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("content", violations.iterator().next().getPropertyPath().toString());
    }
}
