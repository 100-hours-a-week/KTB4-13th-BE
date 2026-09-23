package com.book.core.cart.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.cart.application.usecase.ModifyCartItemUseCase;
import java.sql.SQLException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class CartRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    AddCartItemUseCase addUseCase;

    @Autowired
    GetCartUseCase getCartUseCase;

    @Autowired
    ModifyCartItemUseCase modifyCartItemUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 같은_사용자의_같은_상품_추가는_한_항목의_수량을_대체한다() {
        createCart(1001L);
        createProduct(2001L, 3001L, 10);
        addUseCase.execute(new AddCartItemCommand(1001L, 2001L, 2));
        addUseCase.execute(new AddCartItemCommand(1001L, 2001L, 4));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carts WHERE user_id = ?", Integer.class, 1001L))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                        SELECT COUNT(*)
                        FROM cart_item item
                        JOIN carts cart ON cart.id = item.cart_id
                        WHERE cart.user_id = ? AND item.product_id = ?
                        """, Integer.class, 1001L, 2001L)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                        SELECT item.quantity
                        FROM cart_item item
                        JOIN carts cart ON cart.id = item.cart_id
                        WHERE cart.user_id = ? AND item.product_id = ?
                        """, Integer.class, 1001L, 2001L)).isEqualTo(4);
    }

    @Test
    void 마이그레이션은_수량_범위를_DB에서도_검증한다() {
        createCart(1002L);
        createProduct(2011L, 3011L, 10);
        addUseCase.execute(new AddCartItemCommand(1002L, 2011L, 1));
        final Long cartId = jdbc.queryForObject("SELECT id FROM carts WHERE user_id = ?", Long.class, 1002L);

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO cart_item (cart_id, product_id, quantity) VALUES (?, ?, ?)", cartId, 2003L, 0))
                .isInstanceOf(UncategorizedSQLException.class)
                .rootCause()
                .isInstanceOf(SQLException.class);
    }

    @Test
    void 조회는_활성_항목만_최근_추가순으로_반환한다() {
        createCart(1003L);
        createProduct(2021L, 3021L, 10);
        createProduct(2022L, 3022L, 10);
        createProduct(2023L, 3023L, 10);
        addUseCase.execute(new AddCartItemCommand(1003L, 2021L, 1));
        addUseCase.execute(new AddCartItemCommand(1003L, 2022L, 2));
        addUseCase.execute(new AddCartItemCommand(1003L, 2023L, 3));
        final Long cartId = jdbc.queryForObject("SELECT id FROM carts WHERE user_id = ?", Long.class, 1003L);
        jdbc.update("UPDATE cart_item SET status = 'DELETED' WHERE cart_id = ? AND product_id = ?", cartId, 2021L);

        final var result = getCartUseCase.execute(new GetCartCommand(1003L));

        assertThat(result.items())
                .extracting(GetCartItemResult::productId, GetCartItemResult::quantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2023L, 3), org.assertj.core.groups.Tuple.tuple(2022L, 2));
    }

    @Test
    void 재고가_부족하면_추가하지_않고_기존_장바구니를_보존한다() {
        createCart(1004L);
        createProduct(2031L, 3031L, 2);

        assertThatThrownBy(() -> addUseCase.execute(new AddCartItemCommand(1004L, 2031L, 3)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK));

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cart_item item JOIN carts cart ON cart.id = item.cart_id WHERE cart.user_id = ?",
                        Integer.class,
                        1004L))
                .isZero();
    }

    @Test
    void 수량_변경은_소유권과_활성_상태를_확인하고_실제_수량을_변경한다() {
        createCart(1005L);
        createProduct(2041L, 3041L, 10);
        addUseCase.execute(new AddCartItemCommand(1005L, 2041L, 2));
        final Long cartItemId = jdbc.queryForObject(
                "SELECT item.id FROM cart_item item JOIN carts cart ON cart.id = item.cart_id WHERE cart.user_id = ?",
                Long.class,
                1005L);

        assertThatThrownBy(() -> modifyCartItemUseCase.execute(new ModifyCartItemCommand(9999L, cartItemId, 4)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
        assertThat(quantityOf(cartItemId)).isEqualTo(2);

        modifyCartItemUseCase.execute(new ModifyCartItemCommand(1005L, cartItemId, 4));
        assertThat(quantityOf(cartItemId)).isEqualTo(4);

        jdbc.update("UPDATE cart_item SET status = 'DELETED' WHERE id = ?", cartItemId);
        assertThatThrownBy(() -> modifyCartItemUseCase.execute(new ModifyCartItemCommand(1005L, cartItemId, 5)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
        assertThat(quantityOf(cartItemId)).isEqualTo(4);
    }

    @Test
    void 삭제된_장바구니의_상품은_수량을_변경하지_않는다() {
        createCart(1006L);
        createProduct(2051L, 3051L, 10);
        addUseCase.execute(new AddCartItemCommand(1006L, 2051L, 1));
        final Long cartItemId = jdbc.queryForObject(
                "SELECT item.id FROM cart_item item JOIN carts cart ON cart.id = item.cart_id WHERE cart.user_id = ?",
                Long.class,
                1006L);
        jdbc.update("UPDATE carts SET status = 'DELETED' WHERE user_id = ?", 1006L);

        assertThatThrownBy(() -> modifyCartItemUseCase.execute(new ModifyCartItemCommand(1006L, cartItemId, 2)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
        assertThat(quantityOf(cartItemId)).isEqualTo(1);
    }

    private void createCart(final long userId) {
        jdbc.update("INSERT INTO carts (user_id) VALUES (?)", userId);
    }

    private void createProduct(final long productId, final long bookId, final int stockQuantity) {
        jdbc.update("""
                INSERT INTO books (
                    id, title, author, publisher, category, published_at, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, bookId, "도서" + bookId, "작가", "출판사", "소설", "2026-01-01", "ACTIVE");
        jdbc.update("""
                INSERT INTO products (
                    id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity,
                    status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, productId, bookId, "상품" + productId, 20000.00, 18000.00, 12000.00, stockQuantity);
    }

    private int quantityOf(final Long cartItemId) {
        return jdbc.queryForObject("SELECT quantity FROM cart_item WHERE id = ?", Integer.class, cartItemId);
    }
}
