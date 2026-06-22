# Money Transfer System — Data Flow Diagram (branch `g_c`)

> Reflects only the `g_c` implementation. Render with VS Code Mermaid preview,
> GitHub, or <https://mermaid.live> (export PNG/SVG for slides).
> Pre-rendered PNGs are in [`docs/diagrams/`](./diagrams/).

---

## 1. Level-1 Data Flow Diagram (processes ↔ stores ↔ entities)

```mermaid
graph LR
  USER(["User"])
  ADMIN(["Admin"])
  SMTP(["Gmail SMTP"])

  P1["1.0 Auth & OTP<br/>signup · login 2FA · reset · refresh"]
  P2["2.0 Account, Balance & Bank-link<br/>details · encrypted balance · PDF"]
  P3["3.0 Money Transfer<br/>validate · debit/credit · idempotency"]
  P4["4.0 Rewards<br/>earn points · redeem cashback"]
  P5["5.0 Admin Ops<br/>users / accounts activate · deactivate"]

  DS1[("users")]
  DS2[("accounts")]
  DS3[("transaction_logs")]
  DS4[("bank_details")]
  DS5[("otp_tokens")]
  DS6[("reward_ledger")]

  USER -- "credentials" --> P1
  P1 -- "OTP email" --> SMTP
  P1 -- "JWT access+refresh" --> USER
  P1 <--> DS5
  P1 <--> DS1

  USER -- "balance / statement / link bank" --> P2
  P2 -- "encrypted balance / PDF" --> USER
  P2 <--> DS2
  P2 --> DS3
  P2 <--> DS4
  P2 <--> DS1

  USER -- "transfer request" --> P3
  P3 <--> DS2
  P3 --> DS3
  P3 --> P4

  USER -- "view / redeem rewards" --> P4
  P4 <--> DS1
  P4 <--> DS6
  P4 --> DS2
  P4 --> DS3

  ADMIN -- "manage users & accounts" --> P5
  P5 <--> DS1
  P5 <--> DS2
```

---

## 2. Login (2-factor) + token flow

```mermaid
sequenceDiagram
  actor U as User
  participant FE as Angular SPA
  participant API as AuthController
  participant SEC as AuthManager + UserDetails
  participant OTP as OtpService
  participant MAIL as EmailService
  participant SMTP as Gmail SMTP
  participant DB as MySQL

  U->>FE: username and password
  FE->>API: POST /auth/login
  API->>SEC: authenticate credentials
  SEC->>DB: load user, must be ACTIVE
  API->>OTP: generateAndSendOtp email, LOGIN
  OTP->>DB: upsert otp_tokens
  OTP-->>MAIL: sendOtpEmail async
  MAIL->>SMTP: deliver OTP
  API-->>FE: 200 masked email

  U->>FE: enter OTP
  FE->>API: POST /auth/login/verify-otp username, otp
  Note over FE,API: password NOT resent, already verified in step 1
  API->>DB: find user by username
  API->>OTP: verifyOtp email, otp, LOGIN
  OTP->>DB: delete consumed otp row
  API->>API: load authorities, JwtUtil generate access + refresh
  API-->>FE: 200 accessToken, refreshToken, role, accountId
  FE->>FE: store tokens in sessionStorage
```

---

## 3. Money transfer (validation, idempotency, rewards, failure logging)

```mermaid
sequenceDiagram
  actor U as User
  participant FE as Angular SPA
  participant TC as TransferController
  participant TS as TransferService
  participant AS as AccountService
  participant RS as RewardService
  participant TLS as TransactionLogService
  participant DB as MySQL

  U->>FE: from, to, amount
  FE->>TC: POST /transfers with Bearer JWT
  TC->>TS: transfer request
  TS->>DB: check idempotencyKey in transaction_logs
  TS->>TS: validate amount, not self, not cashback, both ACTIVE, funds

  alt valid
    TS->>AS: load from and to accounts
    TS->>DB: debit + credit accounts, sync bank_details
    TS->>DB: insert SUCCESS transaction_log
    TS->>RS: processReward, same tx, errors swallowed
    RS->>DB: update users.reward_points + insert reward_ledger
    TS-->>TC: TransferResponse SUCCESS + reward
    TC-->>FE: 200 OK
  else invalid
    TS->>TLS: logFailedTransfer, REQUIRES_NEW tx
    TLS->>DB: insert FAILED transaction_log, survives rollback
    TS-->>TC: throw exception
    TC-->>FE: 4xx via GlobalExceptionHandler
  end
```

---

## 4. Balance privacy — AES encryption end-to-end

```mermaid
sequenceDiagram
  actor U as User
  participant FE as Dashboard + CryptoService
  participant API as AccountController
  participant ASVC as AccountService
  participant CS as AesCryptoService

  U->>FE: open dashboard
  FE->>API: GET /accounts/{id}/balance with Bearer JWT
  API->>ASVC: getBalance id
  ASVC->>CS: encrypt balance
  CS-->>ASVC: Base64 of IV + ciphertext
  API-->>FE: 200 ciphertext
  Note over FE: network tab shows only ciphertext, UI shows dots
  U->>FE: click reveal and verify password
  FE->>FE: CryptoService decrypt via Web Crypto, shared secret
  FE-->>U: shows the actual amount
```

---

## 5. Rewards redemption (points → cash)

```mermaid
sequenceDiagram
  actor U as User
  participant FE as Rewards page
  participant RC as RewardController
  participant RS as RewardService
  participant DB as MySQL

  U->>FE: redeem N points
  FE->>RC: POST /rewards/redeem points, Bearer JWT
  RC->>RS: redeem username, points
  RS->>DB: validate min 500, within balance, account linked
  RS->>DB: debit CASHBACK account, credit user account
  RS->>DB: insert SUCCESS transaction_log for cashback
  RS->>DB: decrement users.reward_points
  RS-->>RC: RedeemResponse amountCredited, remainingPoints
  RC-->>FE: 200 OK
```
