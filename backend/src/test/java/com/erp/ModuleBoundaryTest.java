package com.erp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModuleBoundaryTest {
    @Test void modulesRespectDeclaredBoundaries() {
        ApplicationModules.of(ErpBackendApplication.class).verify();
    }
}
