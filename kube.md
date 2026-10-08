# Experimento Kubernetes — SLA Geo Core

Este guia mostra como rodar o `sla-geo-core` num cluster Kubernetes local (minikube), escalar
automaticamente com HPA e fazer teste de carga com o **siege**. Também registra como o ambiente foi
instalado e configurado nesta máquina.

- Como rodar a aplicação fora do Kubernetes: veja o [README.md](README.md).
- Manifests: pasta [`k8s/`](k8s/).

---

## 1. Como o ambiente foi configurado nesta máquina

### Máquina

| Item | Valor |
|---|---|
| SO | Ubuntu 24.04 (kernel 6.17), x86_64 |
| Recursos | 16 CPUs, 31 GB RAM |
| Docker | 29.1.3 (usuário no grupo `docker`, sem sudo) |

### Ferramentas instaladas

| Ferramenta | Versão | Onde | Como foi instalada |
|---|---|---|---|
| `kubectl` | v1.37.1 | `~/.local/bin/kubectl` | binário oficial (`dl.k8s.io`), SHA-256 conferido |
| `minikube` | v1.39.0 | `~/.local/bin/minikube` | binário oficial (`storage.googleapis.com/minikube`), SHA-256 conferido |
| `siege` | (apt) | `/usr/bin/siege` | `sudo apt install -y siege` — **pendente**: exige sudo, rodar manualmente |

`~/.local/bin` já está no `PATH`, então nenhuma das duas primeiras precisou de sudo. Para reinstalar:

```bash
V=$(curl -fsSL https://dl.k8s.io/release/stable.txt)
curl -fsSLO "https://dl.k8s.io/release/$V/bin/linux/amd64/kubectl"
echo "$(curl -fsSL https://dl.k8s.io/release/$V/bin/linux/amd64/kubectl.sha256)  kubectl" | sha256sum -c

curl -fsSLo minikube https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
echo "$(curl -fsSL https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64.sha256)  minikube" | sha256sum -c

install -m 0755 kubectl minikube ~/.local/bin/

sudo apt update && sudo apt install -y siege
```

### Cluster

Criado com:

```bash
minikube start --driver=docker --cpus=4 --memory=8g
minikube addons enable metrics-server
```

| Item | Valor |
|---|---|
| Perfil | `minikube` (único nó, control-plane) |
| Kubernetes | v1.37.0 |
| Driver | `docker` (o "nó" é um contêiner Docker chamado `minikube`) |
| Runtime de contêiner | containerd 2.3.4 |
| CNI | kindnet |
| Recursos do nó | 4 CPUs, 8 GB RAM |
| IP do nó | `192.168.49.2` (confira com `minikube ip`) |
| Addons | `storage-provisioner`, `default-storageclass`, **`metrics-server`** (necessário para `kubectl top` e HPA) |
| kubeconfig | `~/.kube/config`, contexto `minikube` |

### Como o cluster chega no Postgres e no MinIO

O Postgres (`:5435`) e o MinIO (`:9100`) **continuam rodando no host**, fora do cluster. De dentro do
minikube o host é acessível pelo nome **`host.minikube.internal`** (resolve para `192.168.49.1`).
O ConfigMap aponta para esse nome.

```
 host (sua máquina)
 ├── Postgres/PostGIS :5435  ◄──────┐
 ├── MinIO           :9100  ◄──────┤  host.minikube.internal
 ├── siege ──► 192.168.49.2:30090   │
 │              │                    │
 └── contêiner "minikube" (nó k8s)   │
       └── namespace sla-geo         │
             Service NodePort 30090  │
               ├── pod sla-geo-core ─┤
               ├── pod sla-geo-core ─┤   (1 a 5 réplicas via HPA)
               └── pod sla-geo-core ─┘
```

---

## 2. O que está em `k8s/`

| Arquivo | Recurso | Resumo |
|---|---|---|
| `00-namespace.yaml` | Namespace `sla-geo` | isola o experimento |
| `01-configmap.yaml` | ConfigMap `sla-geo-core-config` | `SPRING_DATASOURCE_URL`, `MINIO_ENDPOINT`, `MINIO_BUCKET` apontando para `host.minikube.internal` |
| `02-deployment.yaml` | Deployment `sla-geo-core` | imagem local `sla-geo-core:latest` (`imagePullPolicy: Never`), requests `250m`/`512Mi`, limits `1 CPU`/`1Gi`, probes em `/actuator/health/{liveness,readiness}` |
| `03-service.yaml` | Service `sla-geo-core` (NodePort) | porta `30090` no nó → `9090` no pod |
| `04-hpa.yaml` | HorizontalPodAutoscaler | 1 a 5 réplicas, alvo de **70% de CPU** do request |

O **Secret** `sla-geo-core-secret` (senha do banco e chaves do MinIO) **não fica em arquivo**: é criado
a partir das variáveis de ambiente (passo 3.2) para não versionar credenciais.

> Por que NodePort e não `kubectl port-forward`? O port-forward prende todo o tráfego em **um único pod**;
> com NodePort o Service distribui as requisições entre todas as réplicas, que é o que se quer medir.

---

## 3. Rodando o experimento

Todos os comandos a partir da pasta `core/`.

### 3.1 Ligar o cluster

O minikube **não sobe sozinho** após reiniciar a máquina.

```bash
minikube start
kubectl get nodes          # STATUS deve ser Ready
```

Postgres e MinIO precisam estar no ar no host (ver README).

### 3.2 Criar namespace e Secret

```bash
export SLA_GEO_DB_PASSWORD='<senha-do-postgres>'
export MINIO_ACCESS_KEY='<access-key>'
export MINIO_SECRET_KEY='<secret-key>'

kubectl apply -f k8s/00-namespace.yaml

kubectl -n sla-geo create secret generic sla-geo-core-secret \
  --from-literal=SLA_GEO_DB_PASSWORD="$SLA_GEO_DB_PASSWORD" \
  --from-literal=MINIO_ACCESS_KEY="$MINIO_ACCESS_KEY" \
  --from-literal=MINIO_SECRET_KEY="$MINIO_SECRET_KEY" \
  --dry-run=client -o yaml | kubectl apply -f -
```

(O `--dry-run ... | kubectl apply` permite rodar de novo para atualizar o Secret sem erro.)

### 3.3 Construir a imagem e carregar no cluster

O cluster não enxerga as imagens do Docker do host; é preciso carregá-las:

```bash
docker build -t sla-geo-core:latest .
minikube image load sla-geo-core:latest
```

### 3.4 Deploy

```bash
kubectl apply -f k8s/
kubectl -n sla-geo rollout status deploy/sla-geo-core
kubectl -n sla-geo get pods,svc,hpa
```

### 3.5 Testar

```bash
URL=http://$(minikube ip):30090

curl $URL/actuator/health/readiness        # {"status":"UP"}
curl $URL/api/v1/denominacoes/geometrias
```

`minikube service sla-geo-core -n sla-geo --url` também mostra a URL.

### 3.6 Atualizar após mudar o código

Como a tag é sempre `latest` e não há registry, é preciso recarregar e reiniciar os pods:

```bash
docker build -t sla-geo-core:latest .
minikube image load sla-geo-core:latest
kubectl -n sla-geo rollout restart deploy/sla-geo-core
```

---

## 4. Teste de carga com siege + autoscaling

### 4.1 Preparar a requisição

Endpoint com consulta espacial (bom para gerar carga de CPU e banco):

```bash
URL=http://$(minikube ip):30090

cat > /tmp/body.json <<'EOF'
{"idSolic":1,"idDenom":1,"geoJson":{"type":"Polygon","coordinates":[[[-44.10,-19.95],[-43.85,-19.95],[-43.85,-19.80],[-44.10,-19.80],[-44.10,-19.95]]]}}
EOF
```

### 4.2 Acompanhar (em outro terminal)

```bash
watch -n 5 'kubectl -n sla-geo get hpa; echo; kubectl -n sla-geo top pod'
```

### 4.3 Disparar a carga

```bash
# 32 usuários simultâneos por 3 minutos, sem pausa entre requisições
siege -b -c 32 -t 3M \
  --content-type "application/json" \
  "$URL/api/v1/denominacoes/geometrias/validacoes/municipios/intersecoes POST < /tmp/body.json"
```

Outros exemplos:

```bash
siege -c 10 -t 1M $URL/api/v1/denominacoes/geometrias        # GET simples, com pausas aleatórias
siege -b -c 50 -r 100 $URL/actuator/health                   # 50 usuários x 100 repetições
```

Opções úteis: `-c` usuários simultâneos · `-t` duração (`30S`, `5M`) · `-r` repetições por usuário ·
`-b` benchmark (sem intervalo entre requisições) · `-v` mostra cada requisição.

No fim o siege mostra `Transactions`, `Availability` (%), `Response time`, `Transaction rate` (req/s),
`Concurrency` e `Failed transactions` — anote esses números para comparar cenários (1 réplica fixa
vs. HPA, requests/limits diferentes etc.).

### 4.4 O que esperar

Validado nesta máquina com carga equivalente (32 conexões simultâneas no endpoint acima):

| Tempo | HPA | Réplicas |
|---|---|---|
| ~0 s | `cpu: <unknown>/70%` (métricas ainda sendo coletadas) | 1 |
| ~60 s | `cpu: 165%/70%` | 1 → **3** |
| após a carga | `cpu: 42%/70%` | 3 |

- O HPA leva ~15–60 s para reagir (coleta do metrics-server + ciclo do controlador).
- Pods novos consomem bastante CPU no boot da JVM; isso pode inflar a métrica logo após escalar.
- **Scale-down** é lento de propósito: por padrão o HPA espera **5 minutos** de estabilidade antes de
  reduzir réplicas.
- Cada réplica abre até 10 conexões no Postgres (HikariCP); com 5 réplicas são até 50 conexões.

### 4.5 Variações do experimento

```bash
# fixar réplicas (desligar o HPA)
kubectl -n sla-geo delete hpa sla-geo-core
kubectl -n sla-geo scale deploy/sla-geo-core --replicas=3

# religar o HPA
kubectl apply -f k8s/04-hpa.yaml

# mudar limites sem editar arquivo
kubectl -n sla-geo set resources deploy/sla-geo-core --requests=cpu=500m --limits=cpu=2
```

Para mudanças permanentes, edite os YAMLs em `k8s/` e rode `kubectl apply -f k8s/` de novo.

---

## 5. Comandos do dia a dia

```bash
# estado
kubectl -n sla-geo get all
kubectl -n sla-geo describe pod <pod>
kubectl -n sla-geo get events --sort-by=.lastTimestamp

# logs
kubectl -n sla-geo logs -f deploy/sla-geo-core          # um pod
kubectl -n sla-geo logs -f -l app=sla-geo-core --prefix # todos os pods

# entrar no pod
kubectl -n sla-geo exec -it deploy/sla-geo-core -- sh

# recursos
kubectl top node
kubectl -n sla-geo top pod

# painel web
minikube dashboard
```

## 6. Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| Pod em `CrashLoopBackOff`, log com `Failed to connect ... :9100` | MinIO fora do ar no host | subir o MinIO e `kubectl -n sla-geo rollout restart deploy/sla-geo-core` |
| Log com erro de conexão no Postgres | Postgres fora do ar ou senha errada no Secret | conferir o banco / recriar o Secret (3.2) |
| `ErrImageNeverPull` | imagem não carregada no minikube | `minikube image load sla-geo-core:latest` |
| Pod roda código antigo | imagem `latest` recarregada sem reiniciar os pods | `kubectl -n sla-geo rollout restart deploy/sla-geo-core` |
| HPA com `cpu: <unknown>` | metrics-server ainda coletando, ou addon desligado | esperar ~1 min / `minikube addons enable metrics-server` |
| `kubectl` sem conexão (`connection refused`) | cluster parado | `minikube start` |

## 7. Desligar e limpar

```bash
kubectl delete namespace sla-geo   # remove só o experimento (deployment, service, hpa, secret...)
minikube stop                      # desliga o cluster, mantém o estado
minikube delete                    # apaga o cluster inteiro (recriar com o passo 1)
```
