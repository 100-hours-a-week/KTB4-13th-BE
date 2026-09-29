CREATE TABLE terms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    term_type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    version VARCHAR(20) NOT NULL,
    is_required BIT(1) NOT NULL,
    is_active BIT(1) NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_term_agreement (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    agreed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_term_agreement_user_term_action UNIQUE (user_id, term_id, action),
    CONSTRAINT fk_user_term_agreement_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_term_agreement_term_id FOREIGN KEY (term_id) REFERENCES terms (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO terms (term_type, title, content, version, is_required, is_active, display_order)
SELECT 'PERSONALIZED_RECOMMENDATION',
       '개인화 도서 추천을 위한 정보 수집·이용 동의',
       '이용 목적: 독서 취향 분석 및 개인화 도서 추천 제공
수집·이용 항목: 온보딩 질문 응답, 선택한 관심 도서',
       '1.0',
       b'0',
       b'1',
       1
WHERE NOT EXISTS (SELECT 1 FROM terms WHERE term_type = 'PERSONALIZED_RECOMMENDATION');
