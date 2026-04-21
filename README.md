# Face Recognition API

Serviço responsável por detecção e comparação facial utilizando OpenCV com os modelos **YuNet** e **SFace**.

## Reconhecimento Facial
- **Detecção**: Utiliza o modelo YuNet para localizar faces de forma performática.
- **Reconhecimento**: Utiliza o modelo SFace para extração de embeddings e comparação por similaridade de cosseno.
- **Modelos**: Localizados em `src/main/resources/models`, extraídos para `./models/` em tempo de execução.

## Arquitetura (Adapter Pattern)

Seguindo o design de **Hexagonal/Adapter**, o projeto está estruturado em:
- **`domain.core`**: Contém o `FaceRecognitionPort` (interface) e o `FaceRecognitionAdapter` (implementação do OpenCV).
- **`adapter.controller`**: Controladores que implementam os endpoints REST e interagem com os ports.
- **`infrastructure`**: Configurações de Feign e Tracer para comunicação e rastreabilidade.

## Configurações Principais

- **Porta**: 8082
- **Profiles**: `default`, `local`, `prod`
- **Spring Boot**: 3.5.7 (Kotlin 2.2.21)
- **JDK**: 24
- **OpenCV**: 4.13.0-1.5.13 (via Bytedeco)

## Endpoints (API v1)

- `POST /api/v1/face/compare`: Compara duas imagens via JSON (Base64). Parâmetro `detectSpoofing: true` opcional.
- `POST /api/v1/face/compare/file`: Compara duas imagens via Multipart (Files). Parâmetro `detectSpoofing: true` opcional.
- `POST /api/v1/face/detect`: Detecta faces via JSON (Base64).
- `POST /api/v1/face/detect/file`: Detecta faces via Multipart (File).
- `POST /api/v1/face/detect-spoofing`: Verifica se a imagem é real (Base64).
- `POST /api/v1/face/detect-spoofing/file`: Verifica se o arquivo é real (Multipart).
- `GET /`: Swagger UI da aplicação.

## Detecção de Veracidade (Anti-Spoofing)

O sistema utiliza um pipeline triplo para garantir a integridade da captura:
- **Análise de Bordas Lineares**: Identifica molduras de dispositivos ou bordas de papel usando a Transformada de Hough.
- **Detecção de Moire/Ruído**: Analisa a densidade de bordas (Canny) para identificar padrões de interferência típicos de telas.
- **Análise de Nitidez (Laplaciano)**: Diferencia a nitidez natural de faces reais de capturas re-fotografadas que apresentam borrões ou texturas artificiais.

## Funcionalidades de Infraestrutura

- **OpenFeign**: Preparado para chamadas inter-serviços.
- **Logback Tracer**: Logs estruturados em `.gz` com rotatividade e contador `groupId`.
- **Springdoc**: Swagger completo com descrições e modelos de dados.

## Dependências Relevantes

- `org.bytedeco:opencv-platform`
- `spring-cloud-starter-openfeign`
- `spring-cloud-starter-netflix-eureka-client`
- `springdoc-openapi-starter-webmvc-ui`
- `net.logstash.logback:logstash-logback-encoder`
