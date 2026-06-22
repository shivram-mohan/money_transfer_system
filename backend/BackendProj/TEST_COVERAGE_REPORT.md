# Backend Test Coverage Report

Generated for the `money-transfer-system` Spring Boot module.

## Summary

| Metric                | Result            |
| --------------------- | ----------------- |
| Tests                 | 224 (0 failures)  |
| Instruction Coverage  | **99.73%** (14 of ~5.2k instructions uncovered) |
| Branch Coverage       | **99.05%** (2 of 210 branches uncovered) |
| Class Coverage        | 100%              |
| Method Coverage       | 100%              |

Coverage is measured by JaCoCo (`mvn clean test` → `target/site/jacoco/`).
Lombok-generated members are excluded via `lombok.config`
(`lombok.addLombokGeneratedAnnotation = true`). The two bootstrap mains
(`com.fidelity.Main`, `MoneyTransferApplication`) are excluded in the JaCoCo
plugin config as they contain no testable logic.

## Test strategy

| Layer            | Approach                                                            |
| ---------------- | ------------------------------------------------------------------ |
| Services         | JUnit 5 + Mockito unit tests (all branches, exception paths, boundaries) |
| Controllers      | Standalone `MockMvc` with `GlobalExceptionHandler` advice          |
| Security/JWT     | Unit tests for `JwtUtil`, `JwtAuthFilter`, `JwtAuthEntryPoint`, `CustomUserDetailsService` |
| Config beans     | Direct bean-factory tests (`CorsConfig`, `AsyncConfig`, `UserDetailsServiceImpl`) |
| Entities/DTOs    | Lifecycle callback (`@PrePersist`/`@PreUpdate`) and factory-method tests |
| End-to-end       | `SecurityIntegrationTest` boots the full context on H2 and drives the real filter chain (public access, 401 entry point, JWT auth, admin authorization) |
| Weekend rewards  | `Mockito.mockStatic(LocalDate.class)` to deterministically force weekday/Saturday/Sunday |

## Remaining uncovered code (provably unreachable defensive code)

**2 branches in `JwtUtil`** — the `&& !isTokenExpired(token)` expired-side branch
in both `validateToken` (line 55) and `validateRefreshToken` (line 63). These
methods call `extractUsername(token)` first, which parses the JWT; the jjwt
parser throws `ExpiredJwtException` during parsing for an expired token, so
control never reaches the later `isTokenExpired(token)` check returning `true`.
(`extractUsername` throwing on an expired token is covered by
`JwtUtilTest.expiredToken_throwsOnParse`.)

**2 catch blocks in `AesCryptoService`** (14 instructions) — the
`catch (NoSuchAlgorithmException)` in the constructor and the
`catch (GeneralSecurityException)` in `encrypt`. `SHA-256` is mandated by every
JRE (the constructor catch can never fire), and `AES/CBC/PKCS5Padding` in
*encrypt* mode with a valid key and IV never throws a block-size/padding
exception (those only arise on *decrypt* of malformed input — which **is** covered
by `AesCryptoServiceTest.decrypt_malformedCipherLength_wrapsAsIllegalState`).

All four are honest, harmless dead defensive paths that cannot be exercised
without changing production code, so no contrived tests were added for them.

## Notes on the two recent fixes (see project root `claude.md`)

- **Login verify-OTP** no longer carries the password (`LoginVerifyRequest`
  dropped it); `AuthController.loginVerifyOtp` resolves the user + authorities via
  `UserDetailsService` instead of re-authenticating. Covered by the updated
  `AuthControllerTest` cases (success/user, admin role, invalid OTP, user-not-found).
- **Balance encryption** is provided by `CryptoService`/`AesCryptoService`
  (AES/CBC, SHA-256 key, IV-prepended Base64). User-facing balances
  (`/accounts/{id}`, `/accounts/{id}/balance`, link-bank) are encrypted; admin
  list views stay plaintext. Covered by `AesCryptoServiceTest` plus updated
  `AccountServiceTest`/`UserServiceTest`/controller tests.
