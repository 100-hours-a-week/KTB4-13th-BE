package com.book.core.cart.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemsCommand;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemsUseCase;
import com.book.core.cart.application.usecase.GetCartUseCase;
import java.sql.SQLException;
import java.util.List;
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
    DeleteCartItemUseCase deleteUseCase;

    @Autowired
    DeleteCartItemsUseCase deleteItemsUseCase;

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

    @Test
    void 삭제는_요청_회원의_활성_항목을_DELETED로_변경하고_행을_물리_삭제하지_않는다() {
        createCart(1004L);
        addUseCase.execute(new AddCartItemCommand(1004L, 2004L, 2));
        final Long cartItemId = findCartItemId(1004L, 2004L);

        deleteUseCase.execute(new DeleteCartItemCommand(1004L, cartItemId));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_item WHERE id = ?", Integer.class, cartItemId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM cart_item WHERE id = ?", String.class, cartItemId))
                .isEqualTo("DELETED");
        assertThat(jdbc.queryForObject("SELECT quantity FROM cart_item WHERE id = ?", Integer.class, cartItemId))
                .isEqualTo(2);
    }

    @Test
    void 다른_회원의_상품과_이미_삭제된_상품은_삭제하지_않는다() {
        createCart(1005L);
        createCart(1006L);
        addUseCase.execute(new AddCartItemCommand(1005L, 2005L, 3));
        final Long cartItemId = findCartItemId(1005L, 2005L);

        assertThatThrownBy(() -> deleteUseCase.execute(new DeleteCartItemCommand(1006L, cartItemId)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
        assertThat(jdbc.queryForObject("SELECT status FROM cart_item WHERE id = ?", String.class, cartItemId))
                .isEqualTo("ACTIVE");

        jdbc.update("UPDATE cart_item SET status = 'DELETED' WHERE id = ?", cartItemId);

        assertThatThrownBy(() -> deleteUseCase.execute(new DeleteCartItemCommand(1005L, cartItemId)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_item WHERE id = ?", Integer.class, cartItemId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM cart_item WHERE id = ?", String.class, cartItemId))
                .isEqualTo("DELETED");
    }

    @Test
    void 다건_삭제는_요청_회원의_활성_항목을_DELETED로_변경하고_행을_물리_삭제하지_않는다() {
        createCart(1007L);
        addUseCase.execute(new AddCartItemCommand(1007L, 2007L, 2));
        addUseCase.execute(new AddCartItemCommand(1007L, 2008L, 3));
        final Long firstCartItemId = findCartItemId(1007L, 2007L);
        final Long secondCartItemId = findCartItemId(1007L, 2008L);

        deleteItemsUseCase.execute(new DeleteCartItemsCommand(1007L, List.of(firstCartItemId, secondCartItemId)));

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM cart_item WHERE id IN (?, ?)",
                        Integer.class,
                        firstCartItemId,
                        secondCartItemId))
                .isEqualTo(2);
        assertThat(jdbc.queryForList(
                        "SELECT status FROM cart_item WHERE id IN (?, ?) ORDER BY id",
                        String.class,
                        firstCartItemId,
                        secondCartItemId))
                .containsExactly("DELETED", "DELETED");
        assertThat(jdbc.queryForList(
                        "SELECT quantity FROM cart_item WHERE id IN (?, ?) ORDER BY id",
                        Integer.class,
                        firstCartItemId,
                        secondCartItemId))
                .containsExactly(2, 3);
    }

    @Test
    void 다건_삭제에_소유하지_않거나_이미_삭제된_상품이_포함되면_전체를_변경하지_않는다() {
        createCart(1008L);
        createCart(1009L);
        addUseCase.execute(new AddCartItemCommand(1008L, 2009L, 2));
        addUseCase.execute(new AddCartItemCommand(1009L, 2010L, 3));
        final Long ownedCartItemId = findCartItemId(1008L, 2009L);
        final Long foreignCartItemId = findCartItemId(1009L, 2010L);

        assertThatThrownBy(() -> deleteItemsUseCase.execute(
                        new DeleteCartItemsCommand(1008L, List.of(ownedCartItemId, foreignCartItemId))))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
        assertThat(jdbc.queryForList(
                        "SELECT status FROM cart_item WHERE id IN (?, ?) ORDER BY id",
                        String.class,
                        ownedCartItemId,
                        foreignCartItemId))
                .containsExactly("ACTIVE", "ACTIVE");

        jdbc.update("UPDATE cart_item SET status = 'DELETED' WHERE id = ?", foreignCartItemId);
        assertThatThrownBy(() -> deleteItemsUseCase.execute(
                        new DeleteCartItemsCommand(1008L, List.of(ownedCartItemId, foreignCartItemId))))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
        assertThat(jdbc.queryForList(
                        "SELECT status FROM cart_item WHERE id IN (?, ?) ORDER BY id",
                        String.class,
                        ownedCartItemId,
                        foreignCartItemId))
                .containsExactly("ACTIVE", "DELETED");
    }

    private void createCart(final long userId) {
        jdbc.update("INSERT INTO carts (user_id) VALUES (?)", userId);
    }

    private Long findCartItemId(final long userId, final long productId) {
        return jdbc.queryForObject("""
                        SELECT item.id
                        FROM cart_item item
                        JOIN carts cart ON cart.id = item.cart_id
                        WHERE cart.user_id = ? AND item.product_id = ?
                        """, Long.class, userId, productId);
    }
}
