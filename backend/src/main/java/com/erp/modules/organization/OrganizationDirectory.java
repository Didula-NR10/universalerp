package com.erp.modules.organization;

import java.util.UUID;

public interface OrganizationDirectory {
    UUID branchForWarehouse(UUID warehouseId);
}
