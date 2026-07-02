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

### Backend

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security (planned)
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

## Current Services

### 1. User Service

Responsible for:

* User registration
* User profile management
* Storing user-related metadata

Database:

* PostgreSQL

Main APIs:

* POST /api/users
* GET /api/users/{id}
* GET /api/users

---

### 2. Activity Service

Responsible for:

* Logging activities
* Tracking workout duration
* Maintaining user fitness history

Database:

* MongoDB

Main APIs:

* POST /api/activities
* GET /api/activities
* GET /api/activities/{id}

Supported activity types:

* WALKING
* RUNNING
* CARDIO
* CYCLING

---

### 3. API Gateway (Planned / In Progress)

Responsibilities:

* Single entry point
* Request routing
* Security filters
* Rate limiting

---

### 4. Discovery Server (Planned / In Progress)

Responsibilities:

* Service registration
* Service lookup
* Dynamic routing

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
git clone <your-repo-url>
cd AiFitness
```

## Start databases using Docker

```bash
docker-compose up -d
```

## Run services

Start each service individually:

* User Service
* Activity Service
* Gateway
* Discovery Server

---

# 🧪 API Testing

Use Postman to test endpoints.

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

* JWT authentication
* Kafka event streaming
* Redis caching
* AI-based fitness recommendations
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
