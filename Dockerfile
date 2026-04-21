FROM eclipse-temurin:24-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:24-jre
# O OpenCV via Bytedeco precisa de glibc e bibliotecas nativas que não estão presentes no Alpine.
# Mudamos para uma imagem base baseada em Ubuntu/Debian.
RUN apt-get update && apt-get install -y \
    libgomp1 \
    libquadmath0 \
    libgtk2.0-0 \
    libcanberra-gtk-module \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
