# Implementation plan and gates

| Gate | Current state |
| --- | --- |
| Foundation, toolchain and shell | Implemented; local build passes |
| PostgreSQL schema | Flyway V1–V9 written; execution unverified |
| Identity and application shell | ERP-owned email/password login, local JWT validation, roles and branch selector coded; live login verified, cross-branch tests pending |
| Business modules | Core flows coded across organization, inventory, contacts, procurement, sales/POS, expenses, employees, reporting, notifications and audit |
| Quality and deployment | Foundation/domain tests pass; PostgreSQL integration, security review and deployment remain |

The next gate is a disposable PostgreSQL and Supabase test environment, then the checks in [testing](testing.md). Functional gaps are listed in [status](status.md).
