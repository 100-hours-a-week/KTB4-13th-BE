-- Question / option content is sourced from FE branch feat/9-onboarding-ui
-- (src/features/onboarding/components/*.tsx, src/features/onboarding/mocks/onboardingMockData.ts).
-- That branch was not yet merged (PR KTB4-13th-FE#10) at seed time; re-verify wording once merged.

INSERT INTO onboarding_questions (id, code, content, min_selection, max_selection, display_order)
VALUES
    (1, 'reading-time', '주로 언제 책을 읽으시나요?', 1, 5, 1),
    (2, 'book-criteria', '어떤 기준으로 책을 고르시나요?', 1, 3, 2),
    (3, 'main-category', '관심있는 카테고리를 선택해 주세요', 1, 3, 3),
    (4, 'subcategory', '더 나은 맞춤 추천을 위해, 세부 카테고리를 선택해 주세요', 1, 9, 4);

ALTER TABLE onboarding_questions
    AUTO_INCREMENT = 5;

-- Q1 (question_id = 1)
INSERT INTO onboarding_options (id, onboarding_questions_id, parent_option_id, code, content, display_order)
VALUES
    (1, 1, NULL, 'morning', '아침, 하루를 시작할 때', 1),
    (2, 1, NULL, 'lunch', '점심시간이나 짧은 휴식 시간', 2),
    (3, 1, NULL, 'evening', '저녁, 하루를 마치며', 3),
    (4, 1, NULL, 'before-sleep', '잠들기 전', 4),
    (5, 1, NULL, 'weekend', '주말이나 휴일, 여유로울 때', 5);

-- Q2 (question_id = 2)
INSERT INTO onboarding_options (id, onboarding_questions_id, parent_option_id, code, content, display_order)
VALUES
    (6, 2, NULL, 'publisher', '좋아하는 출판사', 1),
    (7, 2, NULL, 'bestseller', '베스트셀러', 2),
    (8, 2, NULL, 'review', '리뷰·별점 등 대중의 평가', 3);

-- Q3 (question_id = 3) - main categories, no parent
INSERT INTO onboarding_options (id, onboarding_questions_id, parent_option_id, code, content, display_order)
VALUES
    (9, 3, NULL, 'novel', '소설', 1),
    (10, 3, NULL, 'humanities', '인문', 2),
    (11, 3, NULL, 'business', '경제경영', 3),
    (12, 3, NULL, 'self-development', '자기계발', 4),
    (13, 3, NULL, 'essay', '에세이', 5),
    (14, 3, NULL, 'lifestyle', '라이프스타일', 6),
    (15, 3, NULL, 'kids', '어린이', 7),
    (16, 3, NULL, 'science', '과학', 8),
    (17, 3, NULL, 'foreign-language', '외국어', 9),
    (18, 3, NULL, 'philosophy', '철학', 10),
    (19, 3, NULL, 'history', '역사', 11),
    (20, 3, NULL, 'travel', '여행', 12),
    (21, 3, NULL, 'society', '사회', 13),
    (22, 3, NULL, 'it', 'IT', 14);

-- Q4 (question_id = 4) - subcategories, parent_option_id references the Q3 option above
INSERT INTO onboarding_options (id, onboarding_questions_id, parent_option_id, code, content, display_order)
VALUES
    -- novel (parent = 9)
    (23, 4, 9, 'novel-thriller', '추리/스릴러', 1),
    (24, 4, 9, 'novel-sf', 'SF', 2),
    (25, 4, 9, 'novel-fantasy', '판타지', 3),
    (26, 4, 9, 'novel-korean', '한국 소설', 4),
    (27, 4, 9, 'novel-japanese', '일본 소설', 5),
    -- humanities (parent = 10)
    (28, 4, 10, 'humanities-psychology', '심리학', 1),
    (29, 4, 10, 'humanities-reading-writing', '독서/글쓰기', 2),
    (30, 4, 10, 'humanities-general', '인문학', 3),
    (31, 4, 10, 'humanities-language', '언어', 4),
    -- business (parent = 11)
    (32, 4, 11, 'business-economy', '경제', 1),
    (33, 4, 11, 'business-korea-economy', '한국 경제', 2),
    (34, 4, 11, 'business-finance', '재테크', 3),
    (35, 4, 11, 'business-marketing', '마케팅', 4),
    -- self-development (parent = 12)
    (36, 4, 12, 'self-development-habit', '습관', 1),
    (37, 4, 12, 'self-development-career', '커리어', 2),
    (38, 4, 12, 'self-development-leadership', '리더십', 3),
    (39, 4, 12, 'self-development-motivation', '동기부여', 4),
    -- essay (parent = 13)
    (40, 4, 13, 'essay-daily', '일상 에세이', 1),
    (41, 4, 13, 'essay-travel', '여행 에세이', 2),
    (42, 4, 13, 'essay-people', '인물 에세이', 3),
    -- lifestyle (parent = 14)
    (43, 4, 14, 'lifestyle-minimal', '미니멀 라이프', 1),
    (44, 4, 14, 'lifestyle-interior', '인테리어', 2),
    (45, 4, 14, 'lifestyle-hobby', '취미', 3),
    -- kids (parent = 15)
    (46, 4, 15, 'kids-picture-book', '그림책', 1),
    (47, 4, 15, 'kids-fairy-tale', '동화', 2),
    (48, 4, 15, 'kids-comics', '학습 만화', 3),
    -- science (parent = 16)
    (49, 4, 16, 'science-physics', '물리', 1),
    (50, 4, 16, 'science-biology', '생물', 2),
    (51, 4, 16, 'science-space', '우주', 3),
    (52, 4, 16, 'science-brain', '뇌과학', 4),
    -- foreign-language (parent = 17)
    (53, 4, 17, 'foreign-language-english', '영어', 1),
    (54, 4, 17, 'foreign-language-japanese', '일본어', 2),
    (55, 4, 17, 'foreign-language-chinese', '중국어', 3),
    -- philosophy (parent = 18)
    (56, 4, 18, 'philosophy-western', '서양 철학', 1),
    (57, 4, 18, 'philosophy-eastern', '동양 철학', 2),
    (58, 4, 18, 'philosophy-ethics', '윤리학', 3),
    -- history (parent = 19)
    (59, 4, 19, 'history-korea', '한국사', 1),
    (60, 4, 19, 'history-world', '세계사', 2),
    (61, 4, 19, 'history-modern', '근현대사', 3),
    -- travel (parent = 20)
    (62, 4, 20, 'travel-domestic', '국내 여행', 1),
    (63, 4, 20, 'travel-abroad', '해외 여행', 2),
    (64, 4, 20, 'travel-essay', '여행 에세이', 3),
    -- society (parent = 21)
    (65, 4, 21, 'society-issue', '사회 이슈', 1),
    (66, 4, 21, 'society-politics', '정치', 2),
    (67, 4, 21, 'society-environment', '환경', 3),
    -- it (parent = 22)
    (68, 4, 22, 'it-programming', '프로그래밍', 1),
    (69, 4, 22, 'it-ai', 'AI', 2),
    (70, 4, 22, 'it-startup', '스타트업', 3),
    (71, 4, 22, 'it-trend', '테크 트렌드', 4);

ALTER TABLE onboarding_options
    AUTO_INCREMENT = 72;
