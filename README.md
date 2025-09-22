# iPayz Wallet Backend

Spring Boot backend for the iPayz Wallet (Java 17, Spring Boot 3.x)

## What's included
- Spring Boot REST API with JPA (Postgres)
- JWT authentication (jjwt)
- OTP, KYC, Wallet, Transaction skeletons (to be implemented)
- Monnify integration placeholders
- Flyway migrations
- OpenAPI docs (springdoc)
- Docker + docker-compose for local development
- Testcontainers for integration tests

## Prerequisites
- Java 17
- Maven 3.8+
- Docker & Docker Compose (for running DB and app via container)
- (Optional) Postgres locally if not using Docker

## Quick start (with Docker)
1. Copy `.env.example` → `.env` and update environment variables (DB, JWT_SECRET, Monnify keys, SMTP).
2. Start services:
```bash
docker compose up --build
