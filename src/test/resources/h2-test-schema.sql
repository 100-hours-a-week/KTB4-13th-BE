ALTER TABLE IF EXISTS reviews ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS reviews ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS users ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS users ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_providers ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_providers ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS refresh_sessions ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS refresh_sessions ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS onboarding_questions ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS onboarding_questions ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS onboarding_options ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS onboarding_options ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_onboardings ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_onboardings ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_onboarding_answers ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_onboarding_answers ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_onboarding_books ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_onboarding_books ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS recommendation_cards ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS terms ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS terms ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_term_agreement ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_term_agreement ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS coupon_target ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS coupon_target ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS coupon ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS coupon ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS carts ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS carts ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS cart_item ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS cart_item ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS owned_coupon ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS owned_coupon ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_library_books ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_library_books ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_memories ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS user_memories ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS addresses ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS addresses ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS addresses ALTER COLUMN label SET NOT NULL;
ALTER TABLE IF EXISTS addresses ALTER COLUMN postal_code SET NOT NULL;
ALTER TABLE IF EXISTS addresses ALTER COLUMN address SET NOT NULL;
ALTER TABLE IF EXISTS addresses ADD CONSTRAINT ck_addresses_label_not_blank CHECK (CHAR_LENGTH(TRIM(label)) >= 1);
ALTER TABLE IF EXISTS addresses ADD CONSTRAINT ck_addresses_postal_code_not_blank CHECK (CHAR_LENGTH(TRIM(postal_code)) >= 1);
ALTER TABLE IF EXISTS addresses ADD CONSTRAINT ck_addresses_address_not_blank CHECK (CHAR_LENGTH(TRIM(address)) >= 1);
CREATE TABLE IF NOT EXISTS order_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    postal_code VARCHAR(5) NOT NULL,
    address VARCHAR(255) NOT NULL,
    detail_address VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
ALTER TABLE IF EXISTS book_category ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS book_category ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS books ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS books ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS products ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS products ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS product_category ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS product_category ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS order_addresses ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS orders ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS orders ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS order_item ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS order_item ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE IF EXISTS reviews ALTER COLUMN is_spoiler SET DEFAULT FALSE;
ALTER TABLE IF EXISTS product_popularity_snapshots ALTER COLUMN sales_quantity SET DEFAULT 0;
ALTER TABLE IF EXISTS product_popularity_snapshots ALTER COLUMN review_count SET DEFAULT 0;
ALTER TABLE IF EXISTS product_popularity_snapshots ALTER COLUMN review_rate SET DEFAULT 0.00000;

-- Hibernate does not create the generated columns and migration-only indexes
-- that MySQL Flyway migrations add in the production schema.
ALTER TABLE users
    ADD COLUMN active_flag SMALLINT GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END);
ALTER TABLE user_providers
    ADD COLUMN active_flag SMALLINT GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END);
ALTER TABLE refresh_sessions
    ADD COLUMN active_flag SMALLINT GENERATED ALWAYS AS (CASE WHEN revoked_at IS NULL THEN 1 ELSE NULL END);
ALTER TABLE reviews
    ADD COLUMN active_flag SMALLINT GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END);
ALTER TABLE product_category
    ADD COLUMN active_flag SMALLINT GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END);
ALTER TABLE onboarding_options
    ADD COLUMN parent_scope BIGINT GENERATED ALWAYS AS (COALESCE(parent_option_id, 0));
ALTER TABLE user_onboardings
    ADD COLUMN active_flag SMALLINT GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END);

CREATE UNIQUE INDEX uk_user_providers_identity_active_flag
    ON user_providers (provider_type, provider_user_id, active_flag);
ALTER TABLE user_providers
    ADD CONSTRAINT fk_user_providers_user_id FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE refresh_sessions
    ADD CONSTRAINT uk_refresh_sessions_user_active_flag UNIQUE (user_id, active_flag);
ALTER TABLE refresh_sessions
    ADD CONSTRAINT fk_refresh_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE reviews
    ADD CONSTRAINT chk_reviews_rate CHECK (rate BETWEEN 0.0 AND 10.0);
ALTER TABLE product_category
    ADD CONSTRAINT uk_product_category_category_product_active UNIQUE (category_id, product_id, active_flag);
ALTER TABLE onboarding_options
    ADD CONSTRAINT uk_onboarding_options_question_parent_scope_display_order
        UNIQUE (onboarding_questions_id, parent_scope, display_order);
ALTER TABLE onboarding_options
    ADD CONSTRAINT fk_onboarding_options_question FOREIGN KEY (onboarding_questions_id) REFERENCES onboarding_questions (id);
ALTER TABLE onboarding_options
    ADD CONSTRAINT fk_onboarding_options_parent FOREIGN KEY (parent_option_id) REFERENCES onboarding_options (id);
ALTER TABLE user_onboardings
    ADD CONSTRAINT uk_user_onboardings_user_active_flag UNIQUE (user_id, active_flag);
ALTER TABLE user_onboardings
    ADD CONSTRAINT fk_user_onboardings_user_id FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_onboarding_answers
    ADD CONSTRAINT uk_user_onboarding_answers_user_option UNIQUE (user_id, onboarding_option_id);
ALTER TABLE user_onboarding_answers
    ADD CONSTRAINT fk_user_onboarding_answers_user_id FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_onboarding_answers
    ADD CONSTRAINT fk_user_onboarding_answers_option_id FOREIGN KEY (onboarding_option_id) REFERENCES onboarding_options (id);
ALTER TABLE user_onboarding_books
    ADD CONSTRAINT uk_user_onboarding_books_user_book UNIQUE (user_id, book_id);
ALTER TABLE user_onboarding_books
    ADD CONSTRAINT fk_user_onboarding_books_user_id FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_onboarding_books
    ADD CONSTRAINT fk_user_onboarding_books_book_id FOREIGN KEY (book_id) REFERENCES books (id);
ALTER TABLE user_term_agreement
    ADD CONSTRAINT uk_user_term_agreement_user_term_action UNIQUE (user_id, term_id, action);
ALTER TABLE user_term_agreement
    ADD CONSTRAINT fk_user_term_agreement_user_id FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE recommendation_cards
    ADD CONSTRAINT fk_recommendation_cards_user_id FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_term_agreement
    ADD CONSTRAINT fk_user_term_agreement_term_id FOREIGN KEY (term_id) REFERENCES terms (id);
ALTER TABLE user_library_books
    ADD CONSTRAINT uk_user_library_books_user_book UNIQUE (user_id, book_id);
ALTER TABLE user_library_books
    ADD CONSTRAINT fk_user_library_books_user_id FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_library_books
    ADD CONSTRAINT fk_user_library_books_book_id FOREIGN KEY (book_id) REFERENCES books (id);
ALTER TABLE carts
    ADD CONSTRAINT uk_carts_user_id UNIQUE (user_id);
ALTER TABLE cart_item
    ADD CONSTRAINT uk_cart_item_cart_product UNIQUE (cart_id, product_id);
ALTER TABLE cart_item
    ADD CONSTRAINT ck_cart_item_quantity CHECK (quantity BETWEEN 1 AND 500);
ALTER TABLE cart_item
    ADD CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES carts (id);
ALTER TABLE order_item
    ADD CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES products (id);
ALTER TABLE product_popularity_snapshots
    ADD CONSTRAINT fk_product_popularity_snapshots_product FOREIGN KEY (product_id) REFERENCES products (id);
ALTER TABLE onboarding_book_candidates
    ADD CONSTRAINT uk_onboarding_candidates_subcategory_book UNIQUE (subcategory_code, book_id);
ALTER TABLE onboarding_book_candidates
    ADD CONSTRAINT fk_onboarding_candidates_book_id FOREIGN KEY (book_id) REFERENCES books (id);
ALTER TABLE order_item ALTER COLUMN canceled_quantity SET DEFAULT 0;
