---
name: TESTING
description: Test structure, frameworks, and practices
last_mapped_commit: HEAD
---

# TESTING — Test Structure & Practices

**Date:** 2026-06-15  
**Focus:** Quality

## Backend Testing

### Framework
- **JUnit 5** — Test framework
- **Spring Boot Test** — Test annotations and utilities
- **Mockito** — Mocking framework
- **AssertJ** — Fluent assertions

### Test Structure
```
backend/src/test/java/com/resumeforge/
├── auth/
├── resume/
├── job/
├── generation/
└── export/
```

### Test Patterns

#### Controller Tests
```java
@WebMvcTest(ResumeController.class)
@MockBean(ResumeService.class)
```

#### Service Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
```

#### Test Data
- Use `@BeforeEach` for setup
- Repositories for test data persistence
- Clean up in `@AfterEach`

### Running Tests
```bash
cd backend && ./mvnw test
```

## Frontend Testing

### Framework
- **Jest** — Test runner
- **React Testing Library** — Component testing
- **ESLint** — Code quality

### Test Structure
```
frontend/src/
├── __tests__/        # Test files
├── components/       # Component tests
└── hooks/           # Hook tests
```

### Running Tests
```bash
cd frontend && npm run test
cd frontend && npm run build  # Also validates TypeScript
```

### Linting
```bash
cd frontend && npm run lint
```

## Current Test Status

### Backend
- Tests run via Maven: `./mvnw test`
- No visible test files in initial scan — may need verification

### Frontend
- `npm run build` validates TypeScript compilation
- ESLint configured but may not have active tests

## Testing Best Practices

### Backend
1. Test service layer logic (business rules)
2. Mock external dependencies (AI providers, database)
3. Use `@DataJpaTest` for repository tests
4. Test exception scenarios

### Frontend
1. Test component rendering
2. Test user interactions
3. Mock API calls with MSW or Jest mocks
4. Test form validation

## CI/CD

### Backend
- Maven test phase in build
- `./mvnw package -DskipTests` for build-only

### Frontend
- `npm run build` validates TypeScript
- ESLint check available
