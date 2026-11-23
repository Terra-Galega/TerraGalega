# ---------- Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- Run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Railway define PORT; Spring lo lee en server.port
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
