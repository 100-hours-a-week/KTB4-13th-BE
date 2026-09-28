ALTER TABLE products
    ADD CONSTRAINT uk_products_book_id UNIQUE (book_id);
