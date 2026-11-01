# Authentication

The ERP API owns email/password authentication. `local_credential` stores BCrypt password hashes, failed-attempt counters, temporary account locks, a session version and a first-login password-change flag. Supabase is used only as PostgreSQL hosting; Supabase Auth confirmation and publishable keys are no longer required.

`POST /api/v1/auth/login` checks the local credential and returns an eight-hour HS256 JWT. The browser keeps that token in `sessionStorage` and sends it in the Authorization header. On reload, `GET /api/v1/auth/session` validates it. The API validates signature, expiration, issuer, audience, active ERP profile and session version on each request. Logout, administrator password reset and password change increment the session version, invalidating existing tokens.

New users are created by an administrator with a temporary password and then assigned a role. The first administrator on a new database is created with `scripts/bootstrap-local-admin.ps1`; the script is disabled once any local account exists. Temporary passwords must be changed before business permissions are granted. Signed-in users can rotate their own password at `/account/password`. Password reset by email is not implemented; administrators can reset other users, and first-administrator recovery requires an operator with privileged database access.

The foundation profile exposes only status and health. Do not deploy it as the business API. Serve the frontend and API over HTTPS in production, keep `AUTH_JWT_SECRET` in a secret store and rotate it deliberately because rotation invalidates all current tokens.
