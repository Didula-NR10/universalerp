# API overview

Business endpoints under `/api/v1` require an ERP-issued bearer JWT, an active local ERP user and the relevant database role permission. The foundation profile serves only status and health.

| Prefix | Main operations |
| --- | --- |
| `/auth` | Local login, session validation, logout and password change |
| `/company`, `/branches` | Company settings, branches, warehouses and departments |
| `/me`, `/admin/*` | Current profile/permissions, users, roles and assignments |
| `/products`, `/inventory/*` | Catalog, balances, movements and stock operations |
| `/customers`, `/suppliers` | Business contacts |
| `/purchases` | Create, submit, approve, reject, cancel and receive purchase orders |
| `/sales` | Cash sales, full refunds, partial refund requests and decisions |
| `/expenses`, `/expense-categories` | Expenses and approval |
| `/employees`, `/attendance`, `/leave-requests` | Employee and leave workflows |
| `/schedules` | Shift templates, employee availability, draft shifts and approval |
| `/dashboard`, `/reports/*` | Operational metrics, trends and CSV |
| `/notifications`, `/audit` | Notifications and audit history |

Controller source under `backend/src/main/java/com/erp/modules` is the exact route contract. OpenAPI is enabled only in the database-backed profile and is protected by API authentication. The product repository and cash sale/partial refund tests passed with rolled back transactions on the approved ERP test project.
