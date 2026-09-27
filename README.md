# 🎯 InterviewPrep AI — Mock Interview & AI Proctoring Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Security-IDOR%20Protected-blue.svg)](#security--data-isolation)
[![Proctoring](https://img.shields.io/badge/Proctoring-AI%20Facial%20%26%20Fullscreen-red.svg)](#-ai-proctoring-engine)

**InterviewPrep AI** is an intelligent, full-stack mock interview platform powered by AI. It delivers personalized technical interviews, real-time audio/voice evaluation, automated answer scoring, comprehensive proctoring analytics, and an ATS Resume Analyzer.

---

## 🌟 Key Features

### 1. 🤖 Unified AI Chatbot System
A single endpoint (`/api/chat`) operating across three distinct modes:
- **PREP Mode**: Provides direct technical explanations for concepts (e.g., JVM memory layout, Spring annotations, SQL optimization) without non-answer deflections.
- **REVIEW Mode**: Evaluates performance on completed practice sessions, providing per-question feedback.
- **RESUME Mode**: Analyzes missing ATS keywords and suggests high-impact resume improvements.

### 2. 🛡️ AI Proctoring & Exam Integrity Engine
- **Fullscreen Enforcement**: Requires candidates to enter and remain in full-screen mode during exams.
- **Tab & Window Violation Tracking**: Real-time counter (1, 2, 3) tracking window blur and tab switching.
- **Facial Expression Monitoring**: Detects expression events (`LOOKING_AWAY`, `CONFUSED`, `DISTRACTED`).
- **Auto-Termination**: Automatically terminates sessions and generates report cards upon reaching 3 violations.

### 3. 📄 ATS Resume Analyzer & Dynamic Session Generator
- Uploads or pastes candidate resumes and job descriptions.
- Calculates **ATS Match Percentage Score** and identifies missing skill keywords.
- Generates 1-click customized practice interview sessions tailored specifically to candidate skill gaps.

### 4. 📊 Comprehensive Report Cards & Feedback
- Overall score calculation (0 - 100) based on technical accuracy, completeness, and clarity.
- Competency radar breakdown (Technical Skills, Problem Solving, Communication).
- Proctoring violation audit log and question feedback breakdown.

### 5. 🔒 Session Scoping & Data Isolation (IDOR Protection)
- Enforces strict candidate-level ownership validation across all 14 endpoints (Report views, REST APIs, video playback, session deletion, and REVIEW chat).
- Blocks cross-candidate access attempts with **HTTP 403 Forbidden**.

---

## 🛠️ Tech Stack

- **Backend**: Java 17, Spring Boot 3.2.5, Spring Security, Spring Data JPA, Hibernate, H2 Database, Flyway Migrations, Maven.
- **Frontend**: Thymeleaf Templates, Vanilla CSS3 with Custom Properties, Dual Light/Dark Theme, Client-side ES6 JS, Web Speech API (Voice Recognition & Speech Synthesis).
- **AI Integration**: Spring AI Service for multi-criteria answer evaluation and question generation.
- **Containerization**: Docker, Docker Compose, Render / Railway Ready.

---

## 🚀 Getting Started Locally

### Prerequisites
- JDK 17+ installed
- Maven 3.8+ installed

### Build and Run
```bash
# Clone repository
git clone https://github.com/mallacharmi/InterviewPrepAI.git
cd InterviewPrepAI

# Compile & Package JAR
mvn clean package -DskipTests

# Run Spring Boot Application
java -jar target/ai-interview-prep-1.0.0.jar --spring.profiles.active=dev
```
Access the application at `http://localhost:8080`.

---

## 🐳 Docker & Cloud Deployment

### Docker Deployment
```bash
# Build Docker Image
docker build -t interviewprep-ai .

# Run Container
docker run -p 8080:8080 interviewprep-ai
```

### ☁️ 1-Click Render.com Deployment
1. Connect this GitHub repository (`mallacharmi/InterviewPrepAI`) to [Render.com](https://render.com).
2. Render automatically detects `Dockerfile` and `render.yaml`.
3. Deploy to get a permanent 24/7 HTTPS URL (e.g. `https://interviewprep-ai.onrender.com`).

---

## 🧪 Verification & Testing

Run the included automated verification scripts:
```bash
# Verify IDOR Security (14 endpoints)
python test_security_idor.py

# Verify Full System End-to-End Suite
python verify_full_system.py
```

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for details.
