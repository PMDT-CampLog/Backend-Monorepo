# Stage 1: Compilar aplicação
FROM maven:3.9.5-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
# Baixar dependências em cache
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Executar aplicação com JRE leve
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/camplog-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 3333

# Habilitar agente JMX/actuator metrics
ENTRYPOINT ["java", "-jar", "app.jar"]
