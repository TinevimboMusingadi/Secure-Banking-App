# Secure Banking Application

A robust, console-based banking application written in Java that combines core Object-Oriented Programming (OOP) principles with critical secure coding standards. The application features cryptographic authentication, brute-force mitigation, exact financial arithmetic, role-based authorization, and atomic file-based persistence.

---

## Student Details

* **Student Name:** Tinevimbo Musingadi  
* **Registration Number:** H250125B  
* **Course:** Information Security and Assurance (ISA) — Secure Coding  
* **Academic Mini-Project:** 100 Marks  
* **Submission Date:** September 2026  

---

## Features Implemented

### 1. User Authentication & Session Management
* **Cryptographic Password Hashing:** Uses `PBKDF2WithHmacSHA256` with 65,536 iterations and a 128-bit cryptographically secure random salt (`java.security.SecureRandom`). Plaintext passwords are never stored.
* **Brute-Force & Credential-Stuffing Mitigation:** Accounts are locked automatically for 15 minutes after 3 consecutive failed login attempts.
* **Anti-Timing Attack Protection:** Constant-time verification using `MessageDigest.isEqual` and dummy hash execution for non-existent users to eliminate timing side-channel attacks.
* **Memory Hygiene:** Sensitive `char[]` password buffers are immediately overwritten with zeros (`Arrays.fill(..., '\0')`) to minimize exposure in memory dumps.
* **Session Lifecycle:** 15-minute inactivity session expiration and secure logout.

### 2. Account Management
* **Polymorphic Account Creation:** Supports opening **Savings Accounts** (with compound interest calculation and minimum balance enforcement) and **Checking Accounts** (with overdraft protection and overdraft penalty fees).
* **Real-Time Balance Inquiries:** Live account balances formatted to two decimal places.
* **Broken Object Level Authorization (BOLA/IDOR) Defense:** Strict validation ensuring customers can only access and transact on accounts belonging to their own user identity.

### 3. Financial Transactions
* **Deposits & Withdrawals:** Thread-safe monetary operations validating strictly positive amounts within allowable regulatory limits ($0.01 – $1,000,000.00).
* **Inter-Account Transfers:** Atomic debit and credit with deterministic lock acquisition order to prevent race conditions and multi-thread deadlocks.
* **Exact Monetary Precision:** All financial calculations use `java.math.BigDecimal` with `RoundingMode.HALF_UP` to prevent IEEE 754 floating-point rounding vulnerabilities and financial theft exploits.
* **Transaction Receipts & Statements:** Formatted ASCII receipt cards and detailed account statement ledgers with UUIDs and timestamps.

### 4. Data Persistence & Integrity
* **Text-File Persistence:** All users, accounts, transaction ledgers, and audit logs are persisted across application restarts using structured files in `data/`:
  * `data/users.txt`: User credentials, salts, failed attempt counters, and lockout timestamps.
  * `data/accounts.txt`: Account numbers, balances, account types, and attributes.
  * `data/transactions.txt`: Complete append-only transaction history.
  * `data/audit.log`: Tamper-evident security event logs.
* **Atomic File Writes:** Writes to temporary staging files first, then performs atomic file replacements (`StandardCopyOption.ATOMIC_MOVE`) to prevent data corruption during unexpected power cuts or crashes.
* **Delimiter & CSV Injection Protection:** Strict sanitization against newline (`\n`), carriage return (`\r`), and pipe (`|`) injection.

### 5. Role-Based Access Control (RBAC)
* **Customer Role:** Access to account balances, transaction creation, account statements, and password changes for self-owned assets.
* **Administrator Role:** Access to the Security Operations Center (SOC) menu to list all users, review tamper-evident audit logs, and manually unlock locked accounts.

### 6. Creative Console User Interface & "Banking With Musingadi" Branding
* **Signature Branding:** Retro-futuristic ASCII art typography proudly declaring **"BANKING WITH MUSINGADI"** by Tinevimbo Musingadi (Reg No: H250125B).
* **Dynamic Terminal Animations:** Smooth, cinematic progress bars and in-place rotating spinners for startup boot, PBKDF2 key derivation, account creation, and atomic ledger persistence.
* **Dual Password Visibility Modes:** Users can select between **`[1] Hidden Mode`** (masked for confidentiality) and **`[2] Visible Mode`** (plaintext echo to prevent blind typing errors).
* **Password Verification & Mismatch Diagnostics:** Includes an optional reveal/verify preview prompt and side-by-side length/character comparison during registration and confirmation to eliminate unnoticed typos.
* **Formatted Receipts:** Distinctive ASCII transaction receipt cards, secure bank vault doors, and tamper-evident status cards.

---

## Object-Oriented Programming (OOP) Implementation

| OOP Principle | Application in Codebase |
| :--- | :--- |
| **Encapsulation** | All fields across `User`, `Account`, `Transaction`, and `SessionContext` are declared `private` or `protected`. State mutation is only permitted through validated business methods (`deposit()`, `withdraw()`, `recordFailedLogin()`). Defensive copies are returned for collections. |
| **Inheritance** | Abstract class `Account` serves as the base entity for financial accounts, extended by `SavingsAccount` and `CheckingAccount`, reusing core identity and deposit logic. |
| **Polymorphism** | Dynamic method dispatch is used for `withdraw()`, `getAccountType()`, and `applyPeriodicUpdate()`. `SavingsAccount` enforces minimum balance rules, while `CheckingAccount` allows negative balance up to the overdraft limit. |
| **Abstraction** | Core business logic is abstracted into distinct service layers (`AuthService`, `BankingService`, `AuditService`, `FileRepository`), decoupling user interface handling from data storage and cryptographic operations. |

---

## Secure Coding Practices & Threat Mitigation Matrix

| Security Threat / Vulnerability | Mitigated By | Implementation Details |
| :--- | :--- | :--- |
| **Plaintext Credential Exposure** | PBKDF2 Hashing | `PasswordHasher.java`: 65,536 rounds of HMAC-SHA256 with 16-byte random salt. |
| **Brute-Force / Credential Stuffing** | Account Lockout | `User.java` & `AuthService.java`: Locks account after 3 consecutive failures. |
| **Timing Side-Channel Attacks** | Constant-Time Equality | `MessageDigest.isEqual()` and dummy hashes for non-existent users. |
| **Insecure Direct Object Reference (IDOR)** | Object Authorization Check | `BankingService.getAuthorizedAccount()` verifies ownership against active session context. |
| **Floating-Point Rounding Exploits** | Fixed Precision Math | `BigDecimal` with scale 2 and `RoundingMode.HALF_UP` throughout all monetary operations. |
| **Log Injection / Forging (CWE-117)** | Input Sanitization | `AuditService.sanitize()` strips newline and carriage return characters before logging. |
| **Delimiter Injection / Persistence Corruption** | Strict Regex Whitelisting | `InputValidator.java` blocks delimiters (`\|`, `\r`, `\n`) in usernames and names. |
| **Information Disclosure** | Custom Exception Containment | Custom `BankingException` shields internal stack traces from terminal users. |
| **File Write Corruption** | Atomic File Swapping | `FileRepository.atomicWrite()` writes to temporary file then performs atomic rename. |
| **Deadlocks / Concurrency Races** | Deterministic Locking Order | Transfers acquire locks based on lexicographical account number ordering. |

---

## Project Structure

```
Secure-Banking-App/
├── .gitignore
├── README.md
├── build.bat                  # Windows compilation script
├── run.bat                    # Windows launch script
├── data/
│   ├── users.txt              # User credentials & lockout states
│   ├── accounts.txt           # Bank accounts & balances
│   ├── transactions.txt       # Immutable transaction ledger
│   └── audit.log              # Tamper-evident audit trail
└── src/
    └── com/
        └── securebank/
            ├── Main.java                      # Bootstrap & entry point
            ├── model/
            │   ├── Role.java                  # RBAC enum (CUSTOMER, ADMIN)
            │   ├── User.java                  # User domain model
            │   ├── Account.java               # Abstract account base
            │   ├── SavingsAccount.java        # Savings implementation
            │   ├── CheckingAccount.java       # Checking implementation
            │   ├── TransactionType.java       # Transaction categorization
            │   └── Transaction.java           # Immutable transaction record
            ├── security/
            │   ├── PasswordHasher.java        # PBKDF2-HMAC-SHA256 & salt
            │   ├── InputValidator.java        # Regex whitelist validation
            │   └── SessionContext.java        # Session lifecycle & timeout
            ├── repository/
            │   ├── DataPersistenceException.java
            │   └── FileRepository.java        # Atomic file I/O repository
            ├── service/
            │   ├── BankingException.java      # Domain exception
            │   ├── AuditService.java          # Security event logger
            │   ├── AuthService.java           # Auth & lockout logic
            │   └── BankingService.java        # Financial transaction engine
            └── ui/
                ├── AsciiArt.java              # ASCII banners & receipts
                └── ConsoleUI.java             # Terminal user interface
```

---

## How to Build and Run

### Prerequisites
* **Java Development Kit (JDK):** Version 8 or higher (Tested on JDK 17, 21, and 26).
* Standard Windows, Linux, or macOS terminal.

### Quick Start (Windows)

1. **Compile the application:**
   ```bat
   build.bat
   ```
2. **Run the application:**
   ```bat
   run.bat
   ```

### Manual Compilation & Execution (All Platforms)

1. **Compile Java sources:**
   ```bash
   javac -d bin src/com/securebank/model/*.java src/com/securebank/security/*.java src/com/securebank/repository/*.java src/com/securebank/service/*.java src/com/securebank/ui/*.java src/com/securebank/Main.java
   ```
2. **Execute the application:**
   ```bash
   java -cp bin com.securebank.Main
   ```

---

## Default Demo Credentials

The application is pre-seeded with sample secure accounts for immediate evaluation:

| Username | Password | Role | Description |
| :--- | :--- | :--- | :--- |
| `tinevimbo` | `Secure@2026!` | `CUSTOMER` | Student account for Tinevimbo Musingadi (Reg: H250125B) with pre-configured Savings ($1,500.00) & Checking ($500.00) accounts. |
| `admin` | `Admin@2026!` | `ADMIN` | System Security Administrator with access to audit logs, user directory, and account unlock tools. |

---

## Academic Integrity & Authorship Declaration

I hereby declare that this mini-project is entirely my own original work, completed in adherence to the academic standards of the Information Security and Assurance (ISA) program.

**Student:** Tinevimbo Musingadi  
**Registration Number:** H250125B  
**Institution:** University Campus / E-Learning Portal  
