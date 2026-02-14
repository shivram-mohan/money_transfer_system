# 💰 Money Transfer System

A full-stack enterprise banking application with role-based access control, real-time transfers, and comprehensive admin management.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![Angular](https://img.shields.io/badge/Angular-18-red)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)

---

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Testing](#testing)
- [Screenshots](#screenshots)
- [Future Enhancements](#future-enhancements)
- [Contributors](#contributors)

---

## ✨ Features

### User Features
- 🔐 **Secure Authentication** - JWT-based authentication with role-based access control
- 💸 **Money Transfers** - Real-time fund transfers with idempotency protection
- 📊 **Transaction History** - Complete audit trail with account holder names
- 👤 **Self-Registration** - Users can sign up with admin approval workflow
- 💳 **Dashboard** - View balance, recent transactions, quick actions

### Admin Features
- 👥 **User Management** - Create, activate, deactivate users
- ✅ **Approval Workflow** - Review and approve pending user registrations
- 📈 **Analytics Dashboard** - View user statistics and system metrics
- 🔍 **Audit Logs** - Complete transaction visibility across all accounts

### Technical Features
- 🔒 **Security** - BCrypt password encryption, JWT tokens, RBAC
- ⚡ **Performance** - Optimistic locking for concurrent transfers
- 🛡️ **Reliability** - Transaction management with ACID compliance
- 🔄 **Idempotency** - Duplicate transfer prevention
- 🎯 **Validation** - Comprehensive input validation and error handling

---

## 🛠️ Tech Stack

### Backend
- **Java 17** - Core programming language
- **Spring Boot 3.2** - Application framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Database access
- **Hibernate** - ORM framework
- **MySQL 8.0** - Relational database
- **JWT (jjwt 0.11.5)** - Token-based authentication
- **Lombok** - Boilerplate reduction
- **Maven** - Dependency management

### Frontend
- **Angular 18** - Frontend framework
- **TypeScript** - Type-safe JavaScript
- **Angular Material** - UI component library
- **RxJS** - Reactive programming
- **HTTP Interceptors** - Request/response handling

### DevOps & Tools
- **Git** - Version control
- **IntelliJ IDEA** - Backend IDE
- **VS Code** - Frontend IDE
- **Postman** - API testing
- **MySQL Workbench** - Database management

---

## 🏗️ Architecture

### System Architecture
```
┌─────────────────────────────────────────────────────┐
│                   Angular Frontend                  │
│  (Components, Services, Guards, Interceptors)       │
└─────────────────┬───────────────────────────────────┘
                  │ HTTP/REST (JWT)
                  ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot Backend                    │
│  ┌──────────────────────────────────────────────┐  │
│  │  Controllers (REST APIs)                     │  │
│  └──────────────┬───────────────────────────────┘  │
│                 ▼                                   │
│  ┌──────────────────────────────────────────────┐  │
│  │  Services (Business Logic)                   │  │
│  └──────────────┬───────────────────────────────┘  │
│                 ▼                                   │
│  ┌──────────────────────────────────────────────┐  │
│  │  Repositories (Data Access)                  │  │
│  └──────────────┬───────────────────────────────┘  │
└─────────────────┼───────────────────────────────────┘
                  ▼
         ┌─────────────────┐
         │   MySQL Database │
         │  - accounts      │
         │  - users         │
         │  - transactions  │
         └─────────────────┘
```

### Package Structure

**Backend:**
```
com.fidelity.moneytransfer/
├── config/           # Security, CORS, JWT, AOP
├── controller/       # REST endpoints
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── enums/           # Type-safe constants
├── exception/       # Custom exceptions
├── repository/      # Database access
└── service/         # Business logic
```

**Frontend:**
```
src/app/
├── components/      # UI components
│   ├── login/
│   ├── signup/
│   ├── dashboard/
│   ├── transfer/
│   ├── history/
│   └── admin/
├── guards/          # Route protection
├── interceptors/    # HTTP interceptors
├── models/          # TypeScript interfaces
└── services/        # API communication
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Node.js 18+** ([Download](https://nodejs.org/))
- **MySQL 8.0+** ([Download](https://dev.mysql.com/downloads/))
- **Maven 3.6+** (comes with IntelliJ)
- **Git** ([Download](https://git-scm.com/))

### Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/shivram-mohan/money_transfer_system.git
cd money-transfer-system
```

#### 2. Database Setup
```sql
-- Create database
CREATE DATABASE money_transfer_db;

-- Use database
USE money_transfer_db;

-- Tables will be auto-created by Hibernate
-- Run this SQL to create admin user:
INSERT INTO users (
    username, password, name, email, role,
    status, created_date, last_modified_date, created_by
)
VALUES (
    'admin',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', (jwt token)
    'Admin',
    'admin@moneytransfer.com',
    'ADMIN',
    'ACTIVE',
    NOW(), NOW(),
    'system'
);
```

#### 3. Backend Setup
```bash
cd backend/BackendProj

# Update application.yml with your MySQL credentials
# src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/money_transfer_db?createDatabaseIfNotExist=true
    username: root
    password: YOUR_PASSWORD

# Run the application
mvn spring-boot:run

# Backend runs on http://localhost:8080
```

#### 4. Frontend Setup
```bash
cd money-transfer-frontend

# Install dependencies
npm install

# Run development server
ng serve

# Frontend runs on http://localhost:4200
```

---

## 🔑 Default Credentials

### Admin Login
- **Username:** `admin`
- **Password:** `admin123`

### User Signup
Users can self-register at `/signup` and wait for admin approval.

---

## 📡 API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/login` | User login | No |
| POST | `/api/v1/auth/signup` | User registration | No |

### Account Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/accounts/{id}` | Get account details | Yes |
| GET | `/api/v1/accounts/{id}/balance` | Get account balance | Yes |
| GET | `/api/v1/accounts/{id}/transactions` | Get transaction history | Yes |
| GET | `/api/v1/accounts` | Get all accounts (Admin) | Admin |
| POST | `/api/v1/accounts` | Create account (Admin) | Admin |
| PUT | `/api/v1/accounts/{id}/activate` | Activate account (Admin) | Admin |
| PUT | `/api/v1/accounts/{id}/deactivate` | Deactivate account (Admin) | Admin |

### Transfer Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/transfers` | Execute fund transfer | Yes |

### User Management Endpoints (Admin Only)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/users` | Get all users | Admin |
| GET | `/api/v1/users/pending` | Get pending users | Admin |
| GET | `/api/v1/users/{id}` | Get user by ID | Admin |
| POST | `/api/v1/users` | Create user | Admin |
| PUT | `/api/v1/users/{id}/approve` | Approve pending user | Admin |
| DELETE | `/api/v1/users/{id}/reject` | Reject pending user | Admin |
| PUT | `/api/v1/users/{id}/activate` | Activate user | Admin |
| PUT | `/api/v1/users/deactivate` | Deactivate user | Admin |

### Request/Response Examples

**Login Request:**
```json
POST /api/v1/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

**Login Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN",
  "accountId": null,
  "holderName": "Admin",
  "expiresIn": 86400000
}
```

**Transfer Request:**
```json
POST /api/v1/transfers
Authorization: Bearer <token>
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 500.00,
  "idempotencyKey": "unique-key-12345"
}
```

**Transfer Response:**
```json
{
  "transactionId": "TXN-1234567890",
  "status": "SUCCESS",
  "message": "Transfer completed successfully",
  "debitedFrom": 1,
  "creditedTo": 2,
  "amount": 500.00
}
```

---

## 🗄️ Database Schema

### Tables

#### accounts
```sql
CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    holder_name VARCHAR(255) NOT NULL,
    balance DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version INT DEFAULT 0,
    last_updated TIMESTAMP
);
```

#### users
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    account_id BIGINT,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    approved_by VARCHAR(255),
    approved_date TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);
```

#### transaction_logs
```sql
CREATE TABLE transaction_logs (
    id VARCHAR(36) PRIMARY KEY,
    from_account BIGINT,
    to_account BIGINT,
    amount DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(255),
    idempotency_key VARCHAR(100) UNIQUE,
    created_on TIMESTAMP DEFAULT NOW,
    FOREIGN KEY (from_account) REFERENCES accounts(id),
    FOREIGN KEY (to_account) REFERENCES accounts(id)
);
```

### Entity Relationships
```
users (1) ─────> (1) accounts
                      │
                      │
                      ▼
              transaction_logs
```

---

## 🧪 Testing

### Run Backend Tests
```bash
cd backend/BackendProj
mvn test
```

### Test Coverage
- ✅ Unit tests for entity business logic
- ✅ Service layer tests
- ✅ Controller integration tests
- ✅ Repository tests

### Manual Testing with Postman

Import the Postman collection (if available) or test endpoints manually:

1. **Login** - Get JWT token
2. **Create Account** - Admin creates user
3. **Transfer** - Execute fund transfer
4. **History** - View transactions
5. **Admin Operations** - User management

---

## 🎨 Screenshots

### User Dashboard
![Dashboard](docs/screenshots/dashboard.png)

### Money Transfer
![Transfer](docs/screenshots/transfer.png)

### Transaction History
![History](docs/screenshots/history.png)

### Admin Panel
![Admin](docs/screenshots/admin.png)

---

## 🔐 Security Features

### Authentication
- JWT token-based authentication
- Token expiration (24 hours)
- Secure password storage (BCrypt)

### Authorization
- Role-based access control (RBAC)
- Route guards on frontend
- Endpoint protection on backend

### Data Security
- SQL injection prevention (JPA)
- XSS protection
- CORS configuration
- Input validation

### Transaction Security
- Optimistic locking (race condition prevention)
- Idempotency keys (duplicate prevention)
- ACID transaction management

---

## 🚧 Future Enhancements

- [ ] Email notifications on transfers
- [ ] PDF statement generation
- [ ] Transaction date filters (last week/month/year)
- [ ] Account types (Savings/Current)
- [ ] Recurring/scheduled transfers
- [ ] Two-factor authentication (2FA)
- [ ] Mobile app (React Native/Flutter)
- [ ] Analytics dashboard (Snowflake integration)
- [ ] Real-time notifications (WebSocket)
- [ ] Multi-currency support

---

## 📦 Project Structure
```
money-transfer-system/
├── backend/
│   └── BackendProj/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/fidelity/moneytransfer/
│       │   │   └── resources/
│       │   └── test/
│       └── pom.xml
├── frontend/
│   └── money-transfer-frontend/
│       ├── src/
│       │   ├── app/
│       │   ├── assets/
│       │   └── environments/
│       ├── angular.json
│       └── package.json
├── database/
│   └── schema.sql
└── README.md
```

---

## 🐛 Known Issues

- None currently reported

---

## 📝 License

This project is part of a capstone project for educational purposes.

---

## 👥 Contributors

- **Shivram Mohan** - Full Stack Developer
- **Shiva Sai** - Full Stack Developer
- **Shruti** - Full Stack Developer
- **Subhash Krishnasamy** - Full stack developer

---

## 📞 Support

For issues or questions:
- Create an issue in the repository
- Email: [ramshiv590@gmail.com]

---

## 🙏 Acknowledgments

- Fidelity Capstone Project Team
- Spring Boot Documentation
- Angular Material Team

---

**Built with ❤️ using Spring Boot & Angular**
