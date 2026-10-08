# Use a imagem base do Maven com JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build-env

# Defina o diretório de trabalho no contêiner
WORKDIR /app

# Copie o pom.xml e baixe as dependências (fica em cache enquanto o pom não mudar)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copie o restante do código da aplicação
COPY src ./src

# Compile a aplicação (testes pulados: o contextLoads precisa de Postgres e MinIO no ar)
RUN mvn package -DskipTests -B

# Crie uma imagem mínima para execução
FROM eclipse-temurin:17-jre

# Defina o diretório de trabalho no contêiner
WORKDIR /app

# Copie o jar gerado na etapa de build
COPY --from=build-env /app/target/*.jar app.jar

# Variáveis de ambiente com valores padrão (não sensíveis)
# Credenciais (SLA_GEO_DB_PASSWORD, MINIO_ACCESS_KEY, MINIO_SECRET_KEY) devem ser passadas no "docker run"
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/sla-geo-poc \
    MINIO_ENDPOINT=http://127.0.0.1:9100 \
    MINIO_BUCKET=sla-geo-denominacoes

# Exponha a porta que a aplicação vai usar
EXPOSE 9090

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]
