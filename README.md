# SLA Geo Core

API REST (POC) do SLA Geo — Sistema de Licenciamento Ambiental da SEMAD/MG. Recebe, valida e armazena as
geometrias (GeoJSON) das **denominações** e **atividades** de uma solicitação de licenciamento, faz análises
espaciais contra os limites de Minas Gerais e dos municípios (PostGIS), guarda o shapefile original (`.zip`)
no MinIO e consulta critérios locacionais na **IDE-Sisema**.

O front-end correspondente fica em `../wui` (Angular, `http://localhost:4200`, liberado no CORS).

---

## Sumário

- [Stack e versões](#stack-e-versões)
- [Dependências](#dependências)
- [Pré-requisitos](#pré-requisitos)
- [Configuração (variáveis de ambiente)](#configuração-variáveis-de-ambiente)
- [Como rodar](#como-rodar)
- [Endpoints](#endpoints)
- [Arquitetura](#arquitetura)
- [Estrutura de pastas](#estrutura-de-pastas)

---

## Stack e versões

| Item | Versão |
|---|---|
| Java (JDK/JRE) | **17** |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Data JPA | 4.1.1 |
| Hibernate ORM / Hibernate Spatial | 7.4.5.Final |
| Tomcat (embarcado) | 11.0.24 |
| Jackson | 3.1.5 (`tools.jackson`) |
| Maven (wrapper `./mvnw`) | 3.9.16 |
| PostgreSQL + PostGIS | PostgreSQL 18 (SRID **4674** – SIRGAS 2000) |
| MinIO (object storage) | `RELEASE.2025-04-22T22-12-26Z` (SDK Java 8.6.0) |
| Docker (imagens) | `maven:3.9-eclipse-temurin-17` → `eclipse-temurin:17-jre` |

## Dependências

Declaradas no `pom.xml` (parent `spring-boot-starter-parent:4.1.1`):

| Dependência | Versão | Para quê |
|---|---|---|
| `spring-boot-starter-webmvc` | 4.1.1 | API REST (Tomcat embarcado) |
| `spring-boot-starter-data-jpa` | 4.1.1 | Persistência JPA/Hibernate |
| `spring-boot-starter-jdbc` | 4.1.1 | Queries nativas / `JdbcTemplate` (HikariCP 7.0.2) |
| `spring-boot-starter-validation` | 4.1.1 | Bean Validation (`@Valid`, `@NotNull`) |
| `spring-boot-starter-actuator` | 4.1.1 | `/actuator/health` e `/actuator/info` |
| `hibernate-spatial` | 7.4.5.Final | Tipos geométricos no Hibernate |
| `jts-core` / `jts-io-common` | 1.20.0 | Geometrias JTS e leitura/escrita de GeoJSON |
| `postgresql` (runtime) | 42.7.13 | Driver JDBC |
| `minio` | 8.6.0 | Cliente do MinIO |
| `okhttp` | 4.12.0 | Cliente HTTP usado pelo SDK do MinIO |
| `mapstruct` (+ `mapstruct-processor`) | 1.6.3 | Mapeamento entidade ↔ DTO |
| `spring-boot-starter-*-test` (test) | 4.1.1 | Testes (JUnit 6) |

## Pré-requisitos

- **JDK 17** (ex.: `/usr/lib/jvm/java-17-openjdk-amd64`)
- **PostgreSQL com PostGIS** em `localhost:5435`, banco `sla-geo-poc`, usuário `postgres`, contendo:
  - `sla.tb_denominacao`, `sla.tb_atividade`, sequência `sla.seq_atividade`
  - `limite_estadual` e `limite_municipal` (camadas de limites com coluna `geom`)
  - O Hibernate roda com `ddl-auto=validate`: **não cria tabelas**, só confere o esquema.
- **MinIO** em `127.0.0.1:9100` (API) — console em `:9101`. O bucket é criado automaticamente na subida se não existir.
- Docker (opcional, para rodar em contêiner).

> ⚠️ A aplicação **não sobe** se o MinIO estiver fora do ar (`MinioConfig` valida/cria o bucket na
> inicialização). Erro típico: `Failed to connect to /127.0.0.1:9100`. O teste `contextLoads`
> (executado no `verify`) também depende de Postgres e MinIO no ar.

## Configuração (variáveis de ambiente)

Definidas em `src/main/resources/application.properties`:

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `SLA_GEO_DB_PASSWORD` | sim | — | Senha do usuário `postgres` |
| `MINIO_ACCESS_KEY` | sim | — | Access key do MinIO |
| `MINIO_SECRET_KEY` | sim | — | Secret key do MinIO |
| `MINIO_ENDPOINT` | não | `http://127.0.0.1:9100` | Endpoint da API do MinIO |
| `MINIO_BUCKET` | não | `sla-geo-denominacoes` | Bucket dos arquivos `.zip` |
| `IDE_SISEMA_URL` | não | URL de homologação da IDE-Sisema | Base da API de critérios locacionais |
| `IDE_SISEMA_TOKEN` | não | `123` | Token Bearer da IDE-Sisema |
| `SPRING_DATASOURCE_URL` | não | `jdbc:postgresql://localhost:5435/sla-geo-poc` | Sobrescreve a URL do banco |

Porta HTTP: **9090** (`server.port`). Upload máximo: 100 MB por arquivo.

## Como rodar

### 1. Local (on-host)

```bash
cd core

export MINIO_ENDPOINT='http://127.0.0.1:9100'
export MINIO_ACCESS_KEY='<access-key>'
export MINIO_SECRET_KEY='<secret-key>'
export MINIO_BUCKET='sla-geo-denominacoes'
export SLA_GEO_DB_PASSWORD='<senha-do-postgres>'

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

java -version          # deve mostrar 17
./mvnw -version

./mvnw clean verify    # compila + testes
./mvnw spring-boot:run # sobe em http://localhost:9090
```

Para compilar sem rodar os testes (ex.: sem MinIO/Postgres disponíveis): `./mvnw clean package -DskipTests`.

Conferir se subiu:

```bash
curl http://localhost:9090/actuator/health
```

### 2. Docker

O `Dockerfile` é multi-stage (build com Maven/JDK 17, execução com JRE 17). As credenciais **não** ficam
na imagem — são passadas no `docker run`.

```bash
docker build -t sla-geo-core .

# com as variáveis já exportadas no terminal (passo 1):
docker run --rm --network host \
  -e SLA_GEO_DB_PASSWORD -e MINIO_ACCESS_KEY -e MINIO_SECRET_KEY \
  sla-geo-core
```

`--network host` faz o contêiner enxergar `localhost:5435` (Postgres) e `127.0.0.1:9100` (MinIO) da máquina.
Sem ele, sobrescreva os endereços, por exemplo:

```bash
docker run --rm -p 9090:9090 --add-host=host.docker.internal:host-gateway \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5435/sla-geo-poc \
  -e MINIO_ENDPOINT=http://host.docker.internal:9100 \
  -e SLA_GEO_DB_PASSWORD -e MINIO_ACCESS_KEY -e MINIO_SECRET_KEY \
  sla-geo-core
```

## Endpoints

Base: `http://localhost:9090`. Geometrias trafegam como GeoJSON (`geoJson`), em SIRGAS 2000 (EPSG:4674).

### Denominações — `/api/v1/denominacoes/geometrias`

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/` | Cria denominação. `multipart/form-data`: parte `metadata` (JSON) + parte `arquivo` (`.zip`, obrigatório) |
| `PUT` | `/{idDenominacao}` | Atualiza. `metadata` + `arquivo` opcional (substitui o anterior no MinIO) |
| `GET` | `/` | Lista denominações ativas |
| `GET` | `/{idDenominacao}` | Busca por id |
| `GET` | `/solicitacao/{idSolic}` | Lista por solicitação |
| `GET` | `/{idDenominacao}/center` | Ponto interior da geometria `[x, y]` |
| `GET` | `/{idDenominacao}/arquivo` | Download do `.zip` original |
| `DELETE` | `/{idDenominacao}` | Exclusão lógica (`data_excl`) e remoção do arquivo |
| `POST` | `/validacoes/minas-gerais` | Geometria está inteiramente em MG? (`true/false`) |
| `POST` | `/validacoes/municipios/quantidade` | Nº de municípios intersectados |
| `POST` | `/validacoes/municipios/multiplos` | Intersecta mais de um município? |
| `POST` | `/validacoes/municipios/intersecoes` | Municípios intersectados (código IBGE, nome, área m²) |
| `POST` | `/{idDenominacao}/criterios-locacionais` | Consulta a IDE-Sisema e grava o resultado |
| `GET` | `/{idDenominacao}/criterios-locacionais` | Último resultado gravado |

`metadata` / corpo das validações:

```json
{
  "idSolic": 123,
  "idDenom": 1,
  "geoJson": { "type": "Polygon", "coordinates": [[[-43.95,-19.92],[-43.94,-19.92],[-43.94,-19.91],[-43.95,-19.91],[-43.95,-19.92]]] }
}
```

`idDenom` vai de 1 a 12 (ver `DenominacaoEnum`: ADA, AID, AII preliminares, pontos de captação, supressão etc.).

Exemplo de criação:

```bash
curl -X POST http://localhost:9090/api/v1/denominacoes/geometrias \
  -F 'metadata=@meta.json;type=application/json' \
  -F 'arquivo=@shape.zip;type=application/zip'
```

### Atividades — `/api/v1/atividades/geometrias`

A geometria da atividade é a **consolidação** das denominações da solicitação; é criada/atualizada
automaticamente quando denominações são salvas, alteradas ou removidas.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/` | Cria (`{ idSolic, idAtividade, geoJson }`) |
| `PUT` | `/` | Cria ou atualiza |
| `PUT` | `/{idAtividade}` | Atualiza |
| `GET` | `/` | Lista todas |
| `GET` | `/{idAtividade}` | Busca por id |
| `GET` | `/solicitacao/{idSolic}` | Lista por solicitação |
| `GET` | `/{idAtividade}/center` | Ponto interior da geometria |
| `DELETE` | `/{idAtividade}` | Remove |

### Validações — `/api/v1/validacoes`

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/estado` | Geometria da atividade está em MG? |
| `POST` | `/municipios` | Intersecta mais de um município? |
| `POST` | `/municipios/intersecoes` | Municípios intersectados |

### Actuator

`GET /actuator/health` (com detalhes do banco e disco) e `GET /actuator/info`.

Erros de validação de negócio retornam **400** com `{ timestamp, status, error, message }`
(`GlobalExceptionHandler`).

## Arquitetura

Monólito Spring Boot em camadas, com interfaces separadas de implementações em services, repositórios
e integrações:

```
            HTTP (JSON / multipart)
                    │
            ┌───────▼────────┐
            │  controller    │  REST, @Valid, ResponseEntity
            └───────┬────────┘
                    │ DTOs
  ┌─────────────────▼──────────────────┐
  │ application                        │
  │  service (interfaces / impl)       │  regras de negócio, @Transactional
  │  dto · mapper (MapStruct, GeoJSON) │
  └───┬───────────────┬────────────┬───┘
      │               │            │
┌─────▼──────┐  ┌─────▼─────┐ ┌────▼────────────┐
│ repository │  │ model     │ │ integration     │
│ JPA +      │  │ entidades │ │  minio  → MinIO │
│ SQL nativo │  │ enums     │ │  ide    → IDE-  │
│ (PostGIS)  │  │ converter │ │          Sisema │
└─────┬──────┘  └───────────┘ └─────────────────┘
      │
 PostgreSQL/PostGIS  (schema sla + limites)
```

- **controller** — só expõe HTTP; delega aos services.
- **application/service** — regras: valida GeoJSON (JTS), gera o `arquivoId`
  (`{idSolic}_{timestamp}_{idDenom}.zip`), grava no MinIO, persiste e reconsolida a atividade
  (`AtividadeConsolidacaoService`). Se a persistência falhar, o arquivo enviado é removido (compensação).
- **application/mapper** — MapStruct entre entidades e DTOs; `GeoJsonGeometryMapper` converte
  GeoJSON ↔ `Geometry` (JTS, SRID 4674).
- **repository** — Spring Data JPA para CRUD e implementações com SQL nativo PostGIS
  (`ST_Intersects`, `ST_CoveredBy`, `ST_Intersection`, `ST_Area`, `ST_Collect`…) para análises espaciais e consolidação.
- **integration** — clientes externos atrás de interfaces: `MinioStorageClient` (armazenamento do `.zip`)
  e `IdeSisemaClient` (`POST {IDE_SISEMA_URL}/query`, critérios locacionais).
- **config** — `MinioConfig` (cliente + garantia do bucket) e `CorsConfig` (libera `http://localhost:4200`).
- **exception** — `GlobalExceptionHandler` padroniza respostas de erro.

## Estrutura de pastas

```
core/
├── Dockerfile                  # build multi-stage (Maven JDK 17 → JRE 17)
├── .dockerignore
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/       # Maven Wrapper (3.9.16)
└── src/
    ├── main/
    │   ├── java/br/gov/mg/meioambiente/slageo/
    │   │   ├── SlaGeoCoreApplication.java        # main
    │   │   ├── controller/                       # endpoints REST
    │   │   │   ├── AtividadeGeometriaController
    │   │   │   ├── CriteriosLocacionaisController
    │   │   │   ├── DenominacaoGeometriaController
    │   │   │   ├── LimiteEstadualController
    │   │   │   └── LimiteMunicipalController
    │   │   ├── application/
    │   │   │   ├── dto/                          # objetos de entrada/saída da API
    │   │   │   ├── mapper/                       # MapStruct + GeoJSON ↔ JTS
    │   │   │   └── service/
    │   │   │       ├── interfaces/               # contratos dos serviços
    │   │   │       └── impl/                     # regras de negócio
    │   │   ├── model/                            # entidades JPA (sla.tb_denominacao, sla.tb_atividade)
    │   │   │   ├── enums/                        # DenominacaoEnum (tipos 1..12)
    │   │   │   └── converter/                    # conversor JPA do enum
    │   │   ├── repository/
    │   │   │   ├── interfaces/                   # Spring Data + contratos
    │   │   │   └── impl/                         # SQL nativo PostGIS
    │   │   ├── integration/
    │   │   │   ├── minio/                        # cliente de armazenamento
    │   │   │   └── ide/                          # cliente IDE-Sisema
    │   │   ├── config/                           # MinIO, CORS
    │   │   └── exception/                        # tratamento global de erros
    │   └── resources/
    │       └── application.properties
    └── test/java/br/gov/mg/meioambiente/slageo/
        └── SlaGeoCoreApplicationTests.java       # smoke test (sobe o contexto)
```
