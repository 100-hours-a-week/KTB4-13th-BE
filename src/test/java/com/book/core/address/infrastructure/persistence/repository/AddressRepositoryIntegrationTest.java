package com.book.core.address.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.domain.Address;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class AddressRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    AddressRepositoryPort addressRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 주소지의_회원_소유권과_요청_값을_ACTIVE로_저장한다() {
        addressRepository.save(Address.of(10001L, "  집 ", " 12345 ", " 서울시 강남구 ", "   ", true));

        final var row = jdbc.queryForMap(
                "SELECT user_id, label, postal_code, address, detail_address, is_default, status "
                        + "FROM addresses WHERE user_id = ?",
                10001L);

        assertThat(row)
                .containsEntry("user_id", 10001L)
                .containsEntry("label", "집")
                .containsEntry("postal_code", "12345")
                .containsEntry("address", "서울시 강남구")
                .containsEntry("detail_address", null)
                .containsEntry("is_default", true)
                .containsEntry("status", "ACTIVE");
    }

    @Test
    void 활성_주소_개수와_기본_조회는_저장된_ACTIVE_주소를_대상으로_한다() {
        final var activeAddress = Address.of(10002L, "집", "12345", "서울시 강남구", null, true);
        addressRepository.save(activeAddress);

        assertThat(addressRepository.countActiveByUserId(10002L)).isEqualTo(1);
        assertThat(addressRepository.findActiveDefaultByUserId(10002L)).hasValueSatisfying(address -> {
            assertThat(address.userId()).isEqualTo(10002L);
            assertThat(address.label()).isEqualTo("집");
        });
    }

    @Test
    void 활성_주소를_기본_배송지_우선_생성일_오름차순으로_조회한다() {
        jdbc.update(
                "INSERT INTO addresses (user_id, label, postal_code, address, is_default, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                10005L,
                "오래된 주소",
                "12345",
                "서울시 중구",
                false,
                "ACTIVE",
                "2026-01-01 00:00:00",
                "2026-01-01 00:00:00");
        jdbc.update(
                "INSERT INTO addresses (user_id, label, postal_code, address, is_default, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                10005L,
                "기본 주소",
                "12346",
                "서울시 강남구",
                true,
                "ACTIVE",
                "2026-01-02 00:00:00",
                "2026-01-02 00:00:00");
        jdbc.update(
                "INSERT INTO addresses (user_id, label, postal_code, address, is_default, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                10005L,
                "삭제 주소",
                "12347",
                "서울시 서초구",
                true,
                "DELETED",
                "2025-12-31 00:00:00",
                "2025-12-31 00:00:00");
        jdbc.update(
                "INSERT INTO addresses (user_id, label, postal_code, address, is_default, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                10005L,
                "최근 주소",
                "12348",
                "서울시 송파구",
                false,
                "ACTIVE",
                "2026-01-03 00:00:00",
                "2026-01-03 00:00:00");

        assertThat(addressRepository.findActiveByUserId(10005L))
                .extracting(Address::label, Address::isDefaultAddress)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("기본 주소", true),
                        org.assertj.core.groups.Tuple.tuple("오래된 주소", false),
                        org.assertj.core.groups.Tuple.tuple("최근 주소", false));
    }

    @Test
    void 동일_주소와_상세주소의_중복_판정은_별칭까지_확인하고_우편번호는_사용하지_않는다() {
        addressRepository.save(Address.of(10003L, "집", "12345", "서울시 강남구", "101호", false));

        final var sameDetails = Address.of(10003L, "집", "54321", "서울시 강남구", "101호", false);
        final var differentLabel = Address.of(10003L, "회사", "12345", "서울시 강남구", "101호", false);

        assertThat(addressRepository.existsActiveDuplicateAddress(10003L, sameDetails))
                .isTrue();
        assertThat(addressRepository.existsActiveDuplicateAddress(10003L, differentLabel))
                .isFalse();
    }

    @Test
    void 수정_대상은_주소지_ID와_회원_ID가_일치하는_ACTIVE_주소만_조회한다() {
        final Address activeAddress = addressRepository.save(Address.of(10006L, "집", "12345", "서울시 강남구", null, false));
        jdbc.update(
                "INSERT INTO addresses (user_id, label, postal_code, address, is_default, status) "
                        + "VALUES (?, ?, ?, ?, false, 'DELETED')",
                10007L,
                "삭제 주소",
                "12346",
                "서울시 중구");
        final Long deletedAddressId =
                jdbc.queryForObject("SELECT id FROM addresses WHERE user_id = ?", Long.class, 10007L);

        assertThat(addressRepository.findActiveByIdAndUserId(activeAddress.id(), 10006L))
                .hasValueSatisfying(address -> {
                    assertThat(address.id()).isEqualTo(activeAddress.id());
                    assertThat(address.userId()).isEqualTo(10006L);
                });
        assertThat(addressRepository.findActiveByIdAndUserId(activeAddress.id(), 99999L))
                .isEmpty();
        assertThat(addressRepository.findActiveByIdAndUserId(deletedAddressId, 10007L))
                .isEmpty();
    }

    @Test
    void 수정_대상_자기_자신은_중복_주소지로_판정하지_않는다() {
        final Address address = addressRepository.save(Address.of(10008L, "집", "12345", "서울시 강남구", "101호", false));
        final Address sameAddress = new Address(
                address.id(),
                address.userId(),
                address.label(),
                "54321",
                address.address(),
                address.detailAddress(),
                address.isDefaultAddress());

        assertThat(addressRepository.existsActiveDuplicateAddress(10008L, sameAddress))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"label", "postal_code", "address"})
    void 필수_주소지_컬럼의_공백은_DB_제약_예외를_전파한다(final String blankColumn) {
        final Map<String, String> values =
                new HashMap<>(Map.of("label", "집", "postal_code", "12345", "address", "서울시 강남구"));
        values.put(blankColumn, "   ");

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO addresses (user_id, label, postal_code, address, is_default, status) "
                                + "VALUES (?, ?, ?, ?, false, 'ACTIVE')",
                        10004L,
                        values.get("label"),
                        values.get("postal_code"),
                        values.get("address")))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void 주소지_마이그레이션은_deleted_at과_order_addresses를_추가하지_않는다() {
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns "
                                + "WHERE table_schema = DATABASE() AND table_name = 'addresses' AND column_name = 'deleted_at'",
                        Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables "
                                + "WHERE table_schema = DATABASE() AND table_name = 'order_addresses'",
                        Integer.class))
                .isZero();
    }
}
