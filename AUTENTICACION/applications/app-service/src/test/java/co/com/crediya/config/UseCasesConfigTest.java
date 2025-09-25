package co.com.crediya.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.*;

public class UseCasesConfigTest {

    @Test
    void testConfigurationLoadsWithoutErrors() {
        // Test básico: verificar que la configuración se puede cargar sin errores
        assertDoesNotThrow(() -> {
            UseCasesConfig config = new UseCasesConfig();
            assertNotNull(config, "UseCasesConfig should be instantiable");
        });
    }
    @Test
    void testFilterPatternsWorkCorrectly() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(IsolatedTestConfig.class)) {
            String[] beanNames = context.getBeanDefinitionNames();

            // Contar beans que terminan con los patrones esperados
            long useCaseCount = 0, validatorCount = 0, compositeCount = 0, ignoredCount = 0;

            for (String beanName : beanNames) {
                if (beanName.endsWith("UseCase")) {
                    useCaseCount++;
                } else if (beanName.endsWith("Validator")) {
                    validatorCount++;
                } else if (beanName.endsWith("ValidationComposite")) {
                    compositeCount++;
                } else if (beanName.contains("ignored") || beanName.contains("Service")) {
                    ignoredCount++;
                }
            }

            assertTrue(useCaseCount >= 1, "Should find at least 1 UseCase bean");
            assertTrue(validatorCount >= 1, "Should find at least 1 Validator bean");
            assertTrue(compositeCount >= 1, "Should find at least 1 ValidationComposite bean");
            assertEquals(0, ignoredCount, "Should not find any ignored components");
        }
    }
    // Configuración de test aislada que NO escanea los UseCase reales
    // Solo escanea las clases mock que están en este mismo archivo
    @Configuration
    @ComponentScan(
            basePackageClasses = UseCasesConfigTest.class, // Solo escanea este paquete de test
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$"),
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+Validator$"),
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+ValidationComposite$")
            },
            useDefaultFilters = false
    )
    static class IsolatedTestConfig {
        // Esta configuración replica la lógica de UseCasesConfig pero solo para componentes de test
    }

    // Componentes mock SIN dependencias para testing aislado

    @Component
    static class MockUseCase {
        public String execute() {
            return "Mock UseCase executed";
        }
    }

    @Component
    static class MockValidator {
        public boolean validate(String input) {
            return input != null && !input.isEmpty();
        }
    }

    @Component
    static class MockValidationComposite {
        public boolean validateAll(String input) {
            return input != null && input.length() > 0;
        }
    }

    // Este componente debería ser ignorado por los filtros
    @Component
    static class IgnoredService {
        public void doSomething() {
            // Este no debería ser escaneado
        }
    }
}