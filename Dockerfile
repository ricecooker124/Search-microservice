# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests clean package

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /appv

# Quarkus "fast-jar" output
COPY --from=build /app/target/quarkus-app/ /app/quarkus-app/

EXPOSE 8084

# Viktigt: Quarkus kör quarkus-run.jar som finns i quarkus-app/
ENTRYPOINT ["java","-jar","/app/quarkus-app/quarkus-run.jar"]

