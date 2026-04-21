FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:24-jre-alpine
# O OpenCV via Bytedeco pode precisar de algumas bibliotecas nativas, mas o alpine é bem enxuto. 
# Se houver erro de libstdc++ ou glibc, talvez precise trocar para eclipse-temurin:24-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
# Copiar modelos se necessário se não estiverem no jar (no projeto parecem estar em src/main/resources/models)
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
