package com.book;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.book");

    @Test
    void 도메인은_상위_계층과_외부_기술에_의존하지_않는다() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..application..",
                        "..api..",
                        "..infrastructure..",
                        "com.book.support..",
                        "org.springframework..",
                        "jakarta.persistence..",
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
                        "com.book.support..",
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
}
