# Development Guide

## Prerequisites
- Java 21+
- Node.js 18+ & npm
- Docker (Optional)

## Local Development Workflow

### 1. Build and Run Backend
```bash
# Build and execute all tests
./mvnw clean test

# Run Spring Boot backend on port 8080 (dev profile)
./mvnw spring-boot:run
```

### 2. Run Frontend Dashboard
```bash
cd frontend
npm install
npm run dev
# Open http://localhost:5173
```

### 3. Running All Tests
```bash
# Backend unit & integration tests
./mvnw test

# Frontend TypeScript check & build
cd frontend && npm run build
```
