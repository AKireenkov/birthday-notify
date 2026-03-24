# Stage 1: Build React frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Spring Boot backend
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY pom.xml ./
COPY src/ ./src/
# Copy React build into Spring Boot static resources
COPY --from=frontend-build /app/frontend/dist/ ./src/main/resources/static/
RUN mvn package -DskipTests -q

# Stage 3: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
COPY src/main/resources/calendar.json ./calendar.json

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
