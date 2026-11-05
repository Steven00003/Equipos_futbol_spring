# ---- Etapa 1: compilar el jar con Maven y JDK 21 ----
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- Etapa 2: imagen final con JRE ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Solo el jar reempaquetado (el que NO termina en .original)
COPY --from=build /build/target/relaciones_spring_h2-0.0.1-SNAPSHOT.jar app.jar
# Cota de memoria segura para el plan gratis de Render (512 MB): JVM maxHeight 300 MB
ENV JAVA_TOOL_OPTIONS=-Xmx300m
EXPOSE 8118
# Render define PORT en el container; application.properties lo usa con server.port=${PORT:8118}
ENTRYPOINT ["java","-jar","app.jar"]
