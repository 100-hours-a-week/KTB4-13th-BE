CREATE TABLE onboarding_questions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    content VARCHAR(40) NOT NULL,
    min_selection INT NOT NULL,
    max_selection INT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_onboarding_questions_code UNIQUE (code),
    CONSTRAINT uk_onboarding_questions_display_order UNIQUE (display_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE onboarding_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    onboarding_questions_id BIGINT NOT NULL,
    parent_option_id BIGINT NULL,
    code VARCHAR(30) NOT NULL,
    content VARCHAR(30) NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    parent_scope BIGINT GENERATED ALWAYS AS (
        COALESCE(parent_option_id, 0)
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_onboarding_options_question_code UNIQUE (onboarding_questions_id, code),
    CONSTRAINT uk_onboarding_options_question_parent_scope_display_order
        UNIQUE (onboarding_questions_id, parent_scope, display_order),
    CONSTRAINT fk_onboarding_options_question
        FOREIGN KEY (onboarding_questions_id) REFERENCES onboarding_questions (id),
    CONSTRAINT fk_onboarding_options_parent
        FOREIGN KEY (parent_option_id) REFERENCES onboarding_options (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_onboardings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    onboarding_status VARCHAR(20) NOT NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    active_flag TINYINT GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_onboardings_user_active_flag UNIQUE (user_id, active_flag),
    CONSTRAINT fk_user_onboardings_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_onboarding_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    onboarding_option_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_onboarding_answers_user_option UNIQUE (user_id, onboarding_option_id),
    CONSTRAINT fk_user_onboarding_answers_user_id
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_onboarding_answers_option_id
        FOREIGN KEY (onboarding_option_id) REFERENCES onboarding_options (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_onboarding_books (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_onboarding_books_user_book UNIQUE (user_id, book_id),
    CONSTRAINT fk_user_onboarding_books_user_id
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_onboarding_books_book_id
        FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
