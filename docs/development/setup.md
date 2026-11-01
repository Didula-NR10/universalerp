# Cloud database and local authentication setup

The ERP uses Supabase PostgreSQL for business data and its own Spring Boot email/password authentication. Supabase Auth, email confirmation and browser publishable keys are not required. Use JDK 17 and Node.js 20.19+.

## Backend

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and a random Base64 `AUTH_JWT_SECRET` containing at least 32 bytes in the root `.env`. `AUTH_JWT_ISSUER` defaults to `universal-erp`, `AUTH_JWT_AUDIENCE` to `erp`, and `CORS_ALLOWED_ORIGIN` to `http://localhost:5173`. Spring Boot loads the root `.env` when started from `backend` or the project root. Environment variables can override its values. For a local PowerShell terminal:

```powershell
cd 'C:\Users\lenov\OneDrive\Desktop\Sample Pos'
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
cd backend
.\mvnw.cmd spring-boot:run
```

Flyway applies pending migrations and Hibernate validates the schema. V1-V13 are already applied to the approved ERP test project. Check `http://localhost:8080/actuator/health`.

## Frontend

The local `frontend/.env.local` sets `VITE_API_BASE_URL=/api`. Vite proxies `/api` to the backend at `http://localhost:8080`.

```powershell
cd 'C:\Users\lenov\OneDrive\Desktop\Sample Pos\frontend'
npm.cmd ci
npm.cmd run dev
```

Open `http://localhost:5173`. Restart Vite after changing `.env.local`.

## First administrator

On a **new** ERP database with no local accounts, build the backend and run `scripts/bootstrap-local-admin.ps1` from PowerShell. It prompts for the first administrator's email and temporary password, assigns `SUPER_ADMIN`, and refuses to run once an account exists. The user must change the temporary password at first sign-in. On the approved test project, the existing administrator already has a local credential and must change its initial password at first login.

Administrators can create more ERP users in Settings, assign roles and reset another user's password. No Supabase Auth account is needed.
