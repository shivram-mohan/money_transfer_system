# Money Transfer System — Architecture Diagram (branch `g_c`)

> Reflects only what is implemented on branch **`g_c`**: 2-factor OTP auth (login
> step 2 is password-less), JWT access + refresh tokens, AES-encrypted balances,
> points rewards with cashback redemption, PDF statements, and async OTP email.
>
> Render: VS Code "Markdown Preview Mermaid Support", GitHub, or
> <https://mermaid.live> (paste a block to export PNG/SVG for slides).

---

## 1. System Architecture (layered / component view)

```mermaid
graph TB
  subgraph CLIENT["Client — Angular 21 SPA (Angular Material, SSR-capable)"]
    UI["Components<br/>login · signup · dashboard · transfer<br/>history · rewards · admin · forgot-password"]
    GUARDS["Route Guards<br/>auth · user · admin"]
    SVC["Services<br/>auth · account · transfer · reward<br/>crypto (balance decrypt) · inactivity"]
    INT["authInterceptor<br/>attaches Bearer JWT · auto-refresh on 401"]
    GUARDS -.-> UI
    UI --> SVC --> INT
  end

  subgraph API["Spring Boot 3.2 REST API — :8080 /api/v1"]
    subgraph SEC["Security Filter Chain (stateless)"]
      CORS["CorsFilter"]
      JWTF["JwtAuthFilter<br/>validates access token"]
      ENTRY["JwtAuthEntryPoint<br/>401 JSON"]
    end
    subgraph CTRL["Controllers"]
      AC["AuthController"]
      UC["UserController"]
      ACC["AccountController"]
      TC["TransferController"]
      RC["RewardController"]
    end
    subgraph BIZ["Services — business logic"]
      US["UserService"]
      ACS["AccountService"]
      TS["TransferService"]
      RS["RewardService"]
      OS["OtpService"]
      ES["EmailService @Async"]
      PS["PdfService — iText"]
      TLS["TransactionLogService<br/>REQUIRES_NEW"]
      CS["AesCryptoService<br/>balance encryption"]
    end
    subgraph XC["Cross-cutting"]
      JWT["JwtUtil"]
      GEH["GlobalExceptionHandler"]
      AOP["LoggingAspect — AOP"]
      SEED["DataSeeder — CommandLineRunner"]
      ASYNC["AsyncConfig — otpMailExecutor"]
    end
    subgraph REPO["Repositories — Spring Data JPA"]
      R1["UserRepository"]
      R2["AccountRepository"]
      R3["TransactionLogRepository"]
      R4["BankDetailsRepository"]
      R5["OtpTokenRepository"]
      R6["RewardLedgerRepository"]
    end
  end

  subgraph DB["MySQL — money_transfer_db"]
    T1[("users")]
    T2[("accounts")]
    T3[("transaction_logs")]
    T4[("bank_details")]
    T5[("otp_tokens")]
    T6[("reward_ledger")]
  end

  subgraph EXT["External"]
    SMTP["Gmail SMTP — OTP emails"]
  end

  INT -- "HTTPS JSON + Bearer JWT" --> CORS
  CORS --> JWTF --> CTRL
  JWTF -. "no/invalid token" .-> ENTRY
  JWTF --> JWT
  AC --> JWT
  CTRL --> BIZ
  ACS --> CS
  US --> CS
  OS --> ES
  ES --> SMTP
  BIZ --> REPO
  R1 --> T1
  R2 --> T2
  R3 --> T3
  R4 --> T4
  R5 --> T5
  R6 --> T6
```

---

## 2. Request pipeline (how one authenticated call is handled)

```mermaid
graph LR
  A["Angular SPA"] -->|"Bearer JWT"| B["CorsFilter"]
  B --> C["JwtAuthFilter<br/>extract + validate token"]
  C -->|"valid"| D["Controller @RestController"]
  C -->|"missing/invalid"| E["JwtAuthEntryPoint<br/>401 AUTH-401 JSON"]
  D --> F["Service @Transactional<br/>(wrapped by LoggingAspect)"]
  F --> G["JpaRepository"]
  G --> H[("MySQL")]
  F -->|"throws"| I["GlobalExceptionHandler<br/>maps to 4xx/5xx ErrorResponse"]
  D -->|"ResponseEntity"| A
  I --> A
```

---

### Key cross-cutting facts (branch `g_c`)

| Concern | Implementation |
| --- | --- |
| Auth | 2FA: password (login step 1) + email OTP (step 2). Step 2 is **password-less**. |
| Tokens | JWT access (10 min) + refresh (7 days); client auto-refreshes on 401. |
| Balance privacy | `AesCryptoService` encrypts user balances; Angular `CryptoService` decrypts on reveal (admin views stay plaintext). |
| Resilience | Failed transfers logged in a separate `REQUIRES_NEW` tx; reward failures never fail a transfer. |
| Rewards | Points per ₹100 (weekend 2×); redeem ≥500 pts as cash from a corporate CASHBACK account. |
| Seeding | `DataSeeder` seeds admin, sample `bank_details`, and the CASHBACK account at startup. |
