# SpeechMatrix — Speech-to-Text App (Spring Boot)

A full-stack Speech-to-Text application built with Java 17 + Spring Boot 3 + React.

## Tech Stack

| Layer       | Technology                          |
|-------------|-------------------------------------|
| Backend     | Java 17, Spring Boot 3.2            |
| Security    | Spring Security + JWT               |
| Database    | PostgreSQL + Spring Data JPA        |
| STT API     | Deepgram / Google / AssemblyAI      |
| Frontend    | React.js + Vite                     |

## Quick Start

### 1. Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### 2. Database Setup
```sql
CREATE DATABASE sttdb;
```

### 3. Configure
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.password=your_password
app.stt.deepgram.api-key=your_key
```

### 4. Run
```bash
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

## API Endpoints

| Method | Endpoint                  | Auth | Description          |
|--------|---------------------------|------|----------------------|
| POST   | /api/auth/register        | No   | Register user        |
| POST   | /api/auth/login           | No   | Login & get token    |
| POST   | /api/speech/upload        | Yes  | Upload audio file    |
| GET    | /api/speech/history       | Yes  | Get all transcripts  |
| GET    | /api/speech/{id}          | Yes  | Get one transcript   |
| GET    | /api/speech/{id}/export   | Yes  | Download PDF/DOCX    |

## Project Structure

```
src/main/java/com/sttapp/
├── controller/     REST endpoints
├── service/        Business logic
├── repository/     JPA data access
├── model/          JPA entities
├── dto/            Request/response objects
├── config/         Spring config (Security, CORS)
├── security/       JWT filter & utilities
└── exception/      Error handling
```

## Progress

- [x] Day 1 — Project setup, folder structure, entities, auth skeleton
- [ ] Day 2 — Database & JPA complete
- [ ] Day 3 — Auth API (register/login)
- [ ] Day 7 — Audio upload endpoint
- [ ] Day 8 — STT API integration
