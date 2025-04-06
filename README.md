# Three-Layer-Based Security Plagiarism Detection System

This is a Spring Boot-based web application for detecting plagiarism in uploaded documents. It includes user authentication, MAC key generation, and admin approval mechanisms before storing files securely.

## Features

- **User Registration & Login**
- **Admin Login**
- **File Upload (TXT, DOCX, PDF)**
- **MAC Key Generation using Bouncy Castle**
- **Plagiarism Detection using NLP + Cosine Similarity**
- **Admin Approval for Storage**
- **Frontend built with Thymeleaf**
- **Stop Words Filtering for Accurate Similarity**

## Plagiarism Detection Logic

- NLP tokenization using Apache OpenNLP
- Cosine similarity comparison
- Plagiarism flagged if similarity ≥ 75%
- Supports `.txt`, `.docx`, and `.pdf` files

## Technologies Used

- Java 17
- Spring Boot
- Spring Security
- Apache OpenNLP
- Apache PDFBox
- Apache POI (DOCX)
- Bouncy Castle (MAC)
- Thymeleaf
- MySQL
- DriveHQ API (for cloud storage)

## Setup Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/Atharva-099/Three-Layer-Based-Security.git
   cd Three-Layer-Based-Security
2. Set up application.properties: 
  spring.datasource.url=jdbc:mysql://localhost:3306/your_db
spring.datasource.username=your_user
spring.datasource.password=your_password

3.Build and run the application: 
mvn clean install
mvn spring-boot:run

4.Access the app : 
http://localhost:8080


Folder Structure
controller/ – All endpoints (user/admin)

service/ – File processing, similarity, MAC generation

model/ – User, Role, and file-related models

repository/ – Spring Data JPA repositories

templates/ – HTML views (Thymeleaf)

static/ – CSS/JS files
