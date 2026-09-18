package com.book.core.cart.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.cart.application.command.CartAddCommand;
import com.book.core.cart.application.command.CartChangeQuantityCommand;
import com.book.core.cart.application.command.CartDeleteCommand;
import com.book.core.cart.application.port.CartRepository;
import com.book.core.cart.application.usecase.CartAddUseCase;
import com.book.core.cart.application.usecase.CartChangeQuantityUseCase;
import com.book.core.cart.application.usecase.CartDeleteUseCase;
import com.book.core.cart.application.usecase.CartQueryUseCase;
import com.book.core.cart.domain.CartItem;
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
    CartRepository repository;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CartAddUseCase addUseCase;

    @Autowired
    CartChangeQuantityUseCase changeQuantityUseCase;

    @Autowired
    CartDeleteUseCase deleteUseCase;

    @Autowired
    CartQueryUseCase queryUseCase;

    @Test
    void 회원별_장바구니를_한_개만_생성하고_항목을_상품별로_조회한다() {
        final var first = repository.lockOrCreate(1001L);
        final var second = repository.lockOrCreate(1001L);
        repository.saveItem(first.id(), new CartItem(null, 2001L, 2));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repository.findByUserId(1001L).orElseThrow().items())
                .extracting(CartItem::productId, CartItem::quantity)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(2001L, 2));
        assertThat(repository.findByUserId(1002L)).isEmpty();
    }

    @Test
    void 장바구니_추가_수량변경_조회_삭제를_실제_저장소와_연결한다() {
        addUseCase.execute(new CartAddCommand(1005L, 2005L, 2));
        addUseCase.execute(new CartAddCommand(1005L, 2005L, 4));
        final var itemId =
                repository.findByUserId(1005L).orElseThrow().items().getFirst().id();

        assertThat(queryUseCase.execute(1005L).cartItems().getFirst().quantity())
                .isEqualTo(4);
        changeQuantityUseCase.execute(new CartChangeQuantityCommand(1005L, itemId, 3));
        assertThat(queryUseCase.execute(1005L).cartItems().getFirst().quantity())
                .isEqualTo(3);
        changeQuantityUseCase.execute(new CartChangeQuantityCommand(1005L, itemId, 0));
        assertThat(queryUseCase.execute(1005L).cartItems().getFirst().quantity())
                .isEqualTo(1);
        deleteUseCase.execute(new CartDeleteCommand(1005L, List.of(itemId)));
        assertThat(queryUseCase.execute(1005L).cartItems()).isEmpty();
    }

    @Test
    void 장바구니_수량_제약은_DB에서도_검증된다() {
        final var cart = repository.lockOrCreate(1004L);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO cart_item (cart_id, product_id, quantity) VALUES (?, ?, ?)", cart.id(), 2004L, 0))
                .isInstanceOf(UncategorizedSQLException.class)
                .rootCause()
                .isInstanceOf(SQLException.class);
    }
}
