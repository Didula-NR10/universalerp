# Scripts

For a new ERP database with no local credentials, run `bootstrap-local-admin.ps1` after building the backend and applying Flyway migrations. It prompts for the first administrator's email and temporary password, creates the ERP-owned account, and assigns `SUPER_ADMIN`. The password must be changed at first sign-in. The script refuses to run once any local account exists.
