package com.book;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.address.domain.Address;
import com.book.core.book.domain.Book;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import com.book.core.category.domain.Category;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderAddress;
import com.book.core.order.domain.OrderItem;
import com.book.core.product.domain.Product;
import com.book.core.product.domain.ProductCategory;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.book");

    @Test
    void 도메인_모델은_상위_계층과_웹_기술에_의존하지_않는다() {
        noClasses()
                .that()
                .resideInAPackage("com.book.core..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..application..",
                        "..api..",
                        "..infrastructure..",
                        "com.book.common.config..",
                        "com.book.common.logging..",
                        "com.book.common.response..",
                        "org.springframework..",
                        "com.fasterxml.jackson..")
                .check(CLASSES);
    }

    @Test
    void 애플리케이션은_API와_인프라_구현에_의존하지_않는다() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..api..",
                        "..infrastructure..",
                        "com.book.common.config..",
                        "com.book.common.logging..",
                        "com.book.common.response..",
                        "jakarta.persistence..",
                        "org.springframework.data..",
                        "org.springframework.web..")
                .check(CLASSES);
    }

    @Test
    void API_계층은_인프라를_직접_호출하지_않는다() {
        noClasses()
                .that()
                .resideInAPackage("..api..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..")
                .check(CLASSES);
    }

    @Test
    void 공통_코드는_기능에_의존하지_않는다() {
        noClasses()
                .that()
                .resideInAPackage("com.book.common..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.book.core..")
                .check(CLASSES);
    }

    @Test
    void 업무_모델은_JPA_Entity를_겸한다() {
        assertThat(Address.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Cart.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(CartItem.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Category.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Book.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Product.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(ProductCategory.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Order.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(OrderItem.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(OrderAddress.class.isAnnotationPresent(Entity.class)).isTrue();
    }
}
