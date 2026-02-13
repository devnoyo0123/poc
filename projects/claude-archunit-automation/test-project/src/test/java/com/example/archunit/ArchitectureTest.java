package com.example.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit Tests - 아키텍처 규칙 강제화
 *
 * 이 테스트들은 코드베이스가 아키텍처 원칙을 따르는지 검증:
 * - Controllers는 Services에만 의존해야 함
 * - Services는 Repositories에만 의존해야 함
 * - 레어 스키팑 금지 (예: Controller → Repository)
 */
public class ArchitectureTest {

    @Test
    void controllers_should_not_depend_on_repositories_directly() {
        JavaClasses classes = new ClassFileImporter()
            .importPackages("com.example");

        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("..repository..")
            .check(classes);
    }

    @Test
    void services_should_not_access_controllers() {
        JavaClasses classes = new ClassFileImporter()
            .importPackages("com.example");

        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat()
            .resideInAPackage("..controller..")
            .check(classes);
    }
}
