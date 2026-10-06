FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends default-mysql-client ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/businessmanager
RUN mkdir -p /opt/businessmanager/backups /opt/businessmanager/secrets
COPY --from=build /workspace/target/*.jar /opt/businessmanager/businessmanager-backend.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/opt/businessmanager/businessmanager-backend.jar"]
