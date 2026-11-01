# Architecture decisions

1. **Java 17 and Spring Boot 4.1.2.** JDK 17 is installed; Spring Initializr currently generated Boot 4.1.2 with Java 17 and Modulith 2.1.1. Maven Wrapper pins the build tool. No Java toolchain change is required.
2. **Single company.** No tenant ID. Branch access will be enforced in application services and repository queries.
3. **Server-owned data.** React uses Supabase only for authentication; ERP data goes through Spring Boot.
4. **Foundation profile.** A database-free profile makes first startup verifiable without credentials. It contains no fake ERP records or privileged API endpoints. Persistence profiles must fail if their database is unavailable.
5. **Modular monolith.** Modules will expose application interfaces, keep business rules out of controllers, and use transactional domain operations.
