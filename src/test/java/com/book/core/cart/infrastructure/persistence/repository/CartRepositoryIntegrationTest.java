package com.book.core.cart.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.GetCartUseCase;
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
    JdbcTemplate jdbc;

    @Test
    void 같은_사용자의_같은_상품_추가는_한_항목의_수량을_대체한다() {
        createCart(1001L);
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
        addUseCase.execute(new AddCartItemCommand(1002L, 2002L, 1));
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
        addUseCase.execute(new AddCartItemCommand(1003L, 2001L, 1));
        addUseCase.execute(new AddCartItemCommand(1003L, 2002L, 2));
        addUseCase.execute(new AddCartItemCommand(1003L, 2003L, 3));
        final Long cartId = jdbc.queryForObject("SELECT id FROM carts WHERE user_id = ?", Long.class, 1003L);
        jdbc.update("UPDATE cart_item SET status = 'DELETED' WHERE cart_id = ? AND product_id = ?", cartId, 2001L);

        final var result = getCartUseCase.execute(new GetCartCommand(1003L));

        assertThat(result.items())
                .extracting(GetCartItemResult::productId, GetCartItemResult::quantity)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2003L, 3), org.assertj.core.groups.Tuple.tuple(2002L, 2));
    }

    private void createCart(final long userId) {
        jdbc.update("INSERT INTO carts (user_id) VALUES (?)", userId);
    }
}
