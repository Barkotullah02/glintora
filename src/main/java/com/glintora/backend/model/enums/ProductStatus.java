package com.glintora.backend.model.enums;

/**
 * Publication status of a product on the catalogue.
 * Values must stay lowercase to match the CHECK constraint in the schema.
 * Publication status is independent of stock availability.
 */
public enum ProductStatus {
    draft,
    pending_review,
    published,
    hidden,
    discontinued
}
