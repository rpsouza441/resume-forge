# Resume Forge

AI-powered resume builder that helps users create professional, ATS-optimized resumes with real-time feedback and intelligent content suggestions.

## Tech Stack

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green)
![Next.js](https://img.shields.io/badge/Next.js-14-black)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Docker](https://img.shields.io/badge/Docker-Enabled-blue)

## Quick Start

```bash
# 1. Copy environment variables
cp .env.example .env

# 2. Start infrastructure (PostgreSQL)
docker compose up -d postgres

# 3. Install dependencies
npm install

# 4. Start development servers
npm run dev
```

- Backend: http://localhost:8080
- Frontend: http://localhost:3000
- API Docs: http://localhost:8080/swagger-ui.html

**Requirements:**
- Backend: Java 17+ and Maven (or use included mvnw wrapper)
- Frontend: Node.js 18+

## Project Structure

```
resume-forge/
├── backend/                  # Spring Boot Java backend
│   └── src/
│       ├── main/java/com/resumeforge/
│       │   ├── ResumeForgeApplication.java
│       │   ├── config/        # Security, JWT, CORS config
│       │   ├── controller/    # REST API controllers
│       │   ├── service/       # Business logic
│       │   ├── repository/     # JPA repositories
│       │   ├── model/          # JPA entities
│       │   ├── dto/            # Request/response DTOs
│       │   └── exception/      # Global exception handling
│       └── main/resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── application-prod.yml
├── frontend/                 # Next.js 14 TypeScript frontend
│   └── src/
│       ├── app/               # Next.js App Router pages
│       ├── components/        # Reusable UI components
│       ├── hooks/             # Custom React hooks
│       ├── lib/               # API client, utilities
│       ├── providers/          # Context providers
│       └── types/              # TypeScript type definitions
├── specdriven/                # Specification-driven development docs
└── docker-compose.yml
```

## Available Scripts

```bash
npm run dev           # Start all services (backend + frontend) in parallel
npm run dev:backend   # Start only backend (Spring Boot)
npm run dev:frontend  # Start only frontend (Next.js)
npm run build         # Build all packages (backend + frontend)
npm run build:backend # Build only backend JAR
npm run build:frontend # Build only frontend
npm run test          # Run all tests (backend + frontend)
npm run test:backend  # Run only backend tests (Maven)
npm run test:frontend # Run only frontend build (type check + lint + build)
npm run lint          # Lint frontend
npm run clean         # Remove frontend build artifacts
```

## Environment Variables

See `.env.example` for all required environment variables. Copy it to `.env` before running.

## License

MIT
