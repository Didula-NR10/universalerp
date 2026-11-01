# Module boundaries

Spring Modulith modules under `com.erp.modules` are `identity`, `organization`, `inventory`, `contacts`, `procurement`, `sales`, `finance`, `employees`, `reporting`, `notifications` and `audit`. Each owns its service and API surface. Procurement receiving and sales checkout call inventory through the `StockLedger` interface. Inventory publishes low stock notices through the notifications interface.

The module boundary test passes in the foundation build. Database-backed interaction tests are still required.
