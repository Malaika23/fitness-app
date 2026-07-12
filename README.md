# 🏋️ AiFitness — AI Powered Fitness Microservices Platform

AiFitness is a backend-focused microservices project built using Java and Spring Boot to simulate a scalable fitness tracking platform.

The application allows users to:

* Register and manage profiles
* Track fitness activities (walking, running, cardio, etc.)
* Monitor health metrics
* Communicate between distributed services
* Scale services independently using cloud-native architecture

This project is inspired by EmbarkX’s microservices architecture course and extended with additional engineering concepts such as service discovery, observability, and containerization.

---

# 🚀 Tech Stack

### Frontend
* React (v19) & Vite
* Redux Toolkit (State Management)
* Material UI (MUI) & Emotion
* React Router (v8)
* React OAuth2 Code PKCE (Authentication & Login)

### Backend
* Java 21
* Spring Boot
* Spring Web & WebFlux
* Spring Data JPA
* Spring Security & OAuth2 Resource Server
* Hibernate

### Databases

* PostgreSQL (User Service)
* MongoDB (Activity Service)

### Microservices / Cloud

* Spring Cloud Gateway
* Eureka Service Discovery
* Docker
* Kubernetes (planned)

### Monitoring

* Prometheus
* Grafana
* Spring Boot Actuator

### Tools

* IntelliJ IDEA Ultimate
* Docker Desktop
* Postman
* Git + GitHub

---

# 🏗 Architecture

This project follows Microservices Architecture.

Services are independently deployable and communicate via REST APIs.

## Current Services & API Contracts

### 1. User Service (`port: 8081`, Service Name: `USER-SERVICE`)
Handles user profiles, authentication, and validation.

#### **Register User**
* **Endpoint:** `POST /api/user/register`
* **Content-Type:** `application/json`
* **Request Payload:**
  ```json
  {
    "email": "testuser@example.com",
    "password": "testpassword",
    "firstName": "Test",
    "lastName": "User"
  }
  ```
* **Response (200 OK):**
  ```json
  {
    "id": "f182230f-5a96-4fe5-b8a0-50b75402add1",
    "email": "testuser@example.com",
    "firstName": "Test",
    "lastName": "User",
    "role": "USER",
    "createdAt": "2026-07-09T14:10:33.294009",
    "updatedAt": "2026-07-09T14:10:33.294009"
  }
  ```

#### **Get User Profile**
* **Endpoint:** `GET /api/user/{userId}`
* **Response (200 OK):**
  ```json
  {
    "id": "f182230f-5a96-4fe5-b8a0-50b75402add1",
    "email": "testuser@example.com",
    "firstName": "Test",
    "lastName": "User",
    "role": "USER",
    "createdAt": "2026-07-09T14:10:33.294009",
    "updatedAt": "2026-07-09T14:10:33.294009"
  }
  ```

#### **Validate User Existence**
* **Endpoint:** `GET /api/user/{userId}/validate`
* **Response (200 OK):** `true` or `false`

---

### 2. Activity Service (`port: 8082`, Service Name: `ACTIVITYSERVICE`)
Manages fitness logging and publishes events to RabbitMQ exchange `fitness.exchange` (`topic` exchange) under routing key `activity.tracking`.

#### **Create Activity**
* **Endpoint:** `POST /api/activities`
* **Content-Type:** `application/json`
* **Request Payload:**
  ```json
  {
    "userId": "f182230f-5a96-4fe5-b8a0-50b75402add1",
    "type": "RUNNING",
    "duration": 30,
    "caloriesBurned": 300,
    "startTime": "2026-07-09T10:00:00"
  }
  ```
  *(Supported Types: `WALKING`, `RUNNING`, `CARDIO`, `CYCLING`)*
* **Response (201 Created):**
  ```json
  {
    "id": "6a4fac198ae84a7be4c7bbb8",
    "userId": "f182230f-5a96-4fe5-b8a0-50b75402add1",
    "type": "RUNNING",
    "duration": 30,
    "caloriesBurned": 300,
    "startTime": "2026-07-09T10:00:00",
    "additionalMetrics": null,
    "createdAt": "2026-07-09T14:11:37.9114577",
    "updatedAt": "2026-07-09T14:11:37.8843408"
  }
  ```

#### **Get Activity by ID**
* **Endpoint:** `GET /api/activities/{activityId}`
* **Response (200 OK):**
  ```json
  {
    "id": "6a4fac198ae84a7be4c7bbb8",
    "userId": "f182230f-5a96-4fe5-b8a0-50b75402add1",
    "type": "RUNNING",
    "duration": 30,
    "caloriesBurned": 300,
    "startTime": "2026-07-09T10:00:00",
    "additionalMetrics": null,
    "createdAt": "2026-07-09T14:11:37.911",
    "updatedAt": "2026-07-09T14:11:37.884"
  }
  ```

#### **Get Activities by User ID**
* **Endpoint:** `GET /api/activities/user/{userId}`
* **Response (200 OK):** Array of Activity Objects

#### **Delete Activity**
* **Endpoint:** `DELETE /api/activities/{activityId}`
* **Response (204 No Content)**

---

### 3. AI Service (`port: 8083`, Service Name: `AISERVICE`)
Fetches activity metrics and details to generate AI-backed health & fitness recommendations.

#### **Get User Recommendations**
* **Endpoint:** `GET /api/recommendation/user/{userId}`
* **Response (200 OK):**
  ```json
  [
    {
      "id": "6a4fac4c5de75065919df8a3",
      "userId": "f182230f-5a96-4fe5-b8a0-50b75402add1",
      "activityId": "6a4fac198ae84a7be4c7bbb8",
      "activityType": "RUNNING",
      "recommendation": "Great pace! Keep your hydration up and maintain your steady pace.",
      "improvements": null,
      "suggestions": null,
      "safetyMeasures": ["Stretch before running", "Drink water"],
      "createdAt": null
    }
  ]
  ```

#### **Get Activity Recommendation**
* **Endpoint:** `GET /api/recommendation/activity/{activityId}`
* **Response (200 OK):**
  ```json
  {
    "id": "6a4fac4c5de75065919df8a3",
    "userId": "f182230f-5a96-4fe5-b8a0-50b75402add1",
    "activityId": "6a4fac198ae84a7be4c7bbb8",
    "activityType": "RUNNING",
    "recommendation": "Great pace! Keep your hydration up and maintain your steady pace.",
    "improvements": null,
    "suggestions": null,
    "safetyMeasures": ["Stretch before running", "Drink water"],
    "createdAt": null
  }
  ```

---

### 4. Discovery Server (`port: 8761`, Service Name: `eureka`)
Eureka Server responsible for service registration and routing lookup for internal microservice communication.

---

### 5. API Gateway (`port: 8080`, Service Name: `gateway`)
Acts as a single entry point for all client requests, executing:
* Request routing to microservices
* Security filters (JWT validation via Keycloak)
* CORS configuration for the frontend client (listening at `http://localhost:5173`)

---

# 🔄 System Flow

1. Client sends request to Gateway
2. Gateway routes request to microservice
3. Service processes business logic
4. Data stored in database
5. Metrics exposed via Actuator
6. Prometheus scrapes metrics
7. Grafana visualizes dashboards

---

# 📂 Database Design

## User Entity

Fields:

* id
* name
* email
* age
* weight
* height
* createdAt

## Activity Entity

Fields:

* id
* userId
* type
* duration
* caloriesBurned
* date

---

# ⚙️ Local Setup

## Clone repository

```bash
git clone https://github.com/Malaika23/fitness-app.git
cd AiFitness
```

## Start databases using Docker

```bash
docker-compose up -d
```

## Run services

1. Create a `.env` file at the repository root containing your Gemini API credentials for the AI Service:
   ```env
   GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

2. Start the backend services in order (allowing each to boot fully):
   - **Config Server** (`port: 8888`)
   - **Discovery Server (Eureka)** (`port: 8761`)
   - **User Service** (`port: 8081`)
   - **Activity Service** (`port: 8082`)
   - **AI Service** (`port: 8083`)
   - **Gateway** (`port: 8080`)

   To run any service:
   ```bash
   cd <service-folder>
   ./mvnw spring-boot:run
   ```

3. Start the Frontend client:
   ```bash
   cd fitness-app-frontend
   npm install
   npm run dev
   ```
   The client will open at `http://localhost:5173`.

---

# 🧪 Testing

### API Testing
Use Postman to test endpoints.

### E2E Testing
Automated end-to-end integration tests are implemented using Playwright to verify authentication, custom activity logging, and AI recommendations.

To run the automated tests:
```bash
cd fitness-app-frontend
npm run test:e2e
```

Example request:

POST /api/activities

```json
{
  "userId": "1",
  "type": "CARDIO",
  "duration": 60
}
```

Example response:

```json
{
  "id": "activity123",
  "userId": "1",
  "type": "CARDIO",
  "duration": 60
}
```

---

# 📊 Monitoring

Monitoring stack includes:

* Spring Actuator
* Prometheus
* Grafana

Metrics tracked:

* Request count
* Response latency
* Error rate
* JVM memory usage

---

# 🎯 Learning Goals

This project helped me understand:

* Monolith vs Microservices
* Service decomposition
* REST API design
* Database-per-service pattern
* Docker containerization
* Monitoring distributed systems
* Production-grade backend engineering

---

# 🔮 Future Improvements

Planned enhancements:

* Kafka event streaming
* Redis caching
* Workout recommendation engine
* CI/CD pipeline
* Kubernetes deployment
* Resilience4J circuit breaker

---

# 👩‍💻 Author

Malaika Gupta

Backend Engineer with 2.2+ years of experience building scalable backend systems using Java, Spring Boot, SQL, Docker, and distributed systems.

Interested in:

* Backend Engineering
* Distributed Systems
* Cloud Native Development
* Product Thinking
