#!/usr/bin/env python3
"""
Orquestra o experimento de carga do sla-geo-core no minikube usando siege.

Para cada execução (cenário x concorrência x réplicas) coleta:
  - métricas do siege (JSON);
  - CPU consumida e throttling de cada pod (cgroup v2: cpu.stat);
  - CPU consumida pelo PostgreSQL do host (/proc/<pid>/stat);
  - conexões ativas no Postgres (pg_stat_activity, amostradas a cada 1 s).

Uso: python3 run_experiment.py <fase> [<fase> ...]   fases: A A2 B C
Resultados: experiment/results/<fase>.jsonl
"""
import json
import os
import subprocess
import sys
import threading
import time
from pathlib import Path

BASE = Path(__file__).resolve().parent
PAYLOADS = BASE / "payloads"
RESULTS = BASE / "results"
NS = "sla-geo"
DEPLOY = "sla-geo-core"
PG = ["psql", "-h", "localhost", "-p", "5435", "-U", "postgres",
      "-d", "sla-geo-poc", "-X", "-A", "-t", "-c"]
CLK_TCK = os.sysconf("SC_CLK_TCK")

NODE_IP = subprocess.check_output(["minikube", "ip"], text=True).strip()
API = f"http://{NODE_IP}:30090"
DEN = f"{API}/api/v1/denominacoes/geometrias"

SCENARIOS = {
    "S1-health":            f"{API}/actuator/health/liveness",
    "S2-listar":            f"{DEN}",
    "S3-arquivo-minio":     f"{DEN}/24/arquivo",
    "S4-mg-pequena":        f"{DEN}/validacoes/minas-gerais POST < {PAYLOADS}/small.json",
    "S5-intersec-media":    f"{DEN}/validacoes/municipios/intersecoes POST < {PAYLOADS}/medium.json",
    "S6-mg-grande":         f"{DEN}/validacoes/minas-gerais POST < {PAYLOADS}/large.json",
    "S7-intersec-grande":   f"{DEN}/validacoes/municipios/intersecoes POST < {PAYLOADS}/large.json",
}

SIEGERC = BASE / "siegerc"


def sh(cmd, **kw):
    return subprocess.run(cmd, check=True, capture_output=True, text=True, **kw).stdout


# ---------------------------------------------------------------- métricas
def pods():
    out = sh(["kubectl", "-n", NS, "get", "pods", "-l", f"app={DEPLOY}",
              "--field-selector=status.phase=Running", "-o",
              "jsonpath={.items[*].metadata.name}"])
    return out.split()


def pod_cpu_stat(pod):
    out = sh(["kubectl", "-n", NS, "exec", pod, "--", "cat", "/sys/fs/cgroup/cpu.stat"])
    return {k: int(v) for k, v in (line.split() for line in out.splitlines())}


def pods_cpu():
    return {p: pod_cpu_stat(p) for p in pods()}


def pg_cpu_ticks():
    total = 0
    for pid in os.listdir("/proc"):
        if not pid.isdigit():
            continue
        try:
            with open(f"/proc/{pid}/stat") as f:
                stat = f.read()
        except OSError:
            continue
        comm = stat[stat.index("(") + 1:stat.rindex(")")]
        if comm.startswith("postgres"):
            fields = stat[stat.rindex(")") + 2:].split()
            total += int(fields[11]) + int(fields[12])  # utime + stime
            if fields[1] == "1" or fields[1] != "0" and _is_postmaster(fields):
                # CPU de backends já encerrados é acumulada no postmaster
                total += int(fields[13]) + int(fields[14])  # cutime + cstime
    return total


def _is_postmaster(fields):
    try:
        with open(f"/proc/{fields[1]}/stat") as f:
            parent = f.read()
        return "(postgres)" not in parent
    except OSError:
        return False


class PgSampler(threading.Thread):
    SQL = ("select count(*) filter (where state='active'), count(*) "
           "from pg_stat_activity where datname='sla-geo-poc' "
           "and pid <> pg_backend_pid()")

    def __init__(self):
        super().__init__(daemon=True)
        self.samples = []
        self.stop = threading.Event()

    def run(self):
        while not self.stop.is_set():
            try:
                a, t = sh(PG + [self.SQL]).strip().split("|")
                self.samples.append((int(a), int(t)))
            except Exception:
                pass
            self.stop.wait(1)


# ---------------------------------------------------------------- execução
def set_replicas(n):
    subprocess.run(["kubectl", "-n", NS, "delete", "hpa", DEPLOY,
                    "--ignore-not-found"], check=True, capture_output=True)
    sh(["kubectl", "-n", NS, "scale", f"deploy/{DEPLOY}", f"--replicas={n}"])
    sh(["kubectl", "-n", NS, "rollout", "status", f"deploy/{DEPLOY}", "--timeout=300s"])
    while len(pods()) != n:
        time.sleep(2)
    time.sleep(5)


def siege(target, concurrency, duration):
    cmd = ["siege", "-R", str(SIEGERC), "-b", "-j",
           "-c", str(concurrency), "-t", duration,
           "-T", "application/json", target]
    p = subprocess.run(cmd, capture_output=True, text=True)
    out = p.stdout
    return json.loads(out[out.index("{"):out.rindex("}") + 1])


def run_one(phase, scenario, concurrency, duration, replicas, out):
    target = SCENARIOS[scenario]
    before_pods, before_pg = pods_cpu(), pg_cpu_ticks()
    sampler = PgSampler()
    sampler.start()
    t0 = time.time()
    s = siege(target, concurrency, duration)
    elapsed = time.time() - t0
    sampler.stop.set()
    sampler.join()
    after_pods, after_pg = pods_cpu(), pg_cpu_ticks()

    pod_cpu_s = throttled_s = periods = throttled = 0
    for p, a in after_pods.items():
        b = before_pods.get(p, {k: 0 for k in a})
        pod_cpu_s += (a["usage_usec"] - b["usage_usec"]) / 1e6
        throttled_s += (a["throttled_usec"] - b["throttled_usec"]) / 1e6
        periods += a["nr_periods"] - b["nr_periods"]
        throttled += a["nr_throttled"] - b["nr_throttled"]
    pg_cpu_s = (after_pg - before_pg) / CLK_TCK
    act = [a for a, _ in sampler.samples] or [0]
    tot = [t for _, t in sampler.samples] or [0]

    row = {
        "phase": phase, "scenario": scenario, "users": concurrency,
        "replicas": replicas, "duration": duration, "elapsed_s": round(elapsed, 2),
        **{k: s.get(k) for k in (
            "transactions", "availability", "response_time", "transaction_rate",
            "throughput", "successful_transactions",
            "failed_transactions", "longest_transaction", "shortest_transaction")},
        "siege_concurrency": s.get("concurrency"),
        "pod_cpu_cores": round(pod_cpu_s / elapsed, 3),
        "pod_throttle_pct": round(100 * throttled / periods, 1) if periods else 0,
        "pod_throttled_s": round(throttled_s, 2),
        "pg_cpu_cores": round(pg_cpu_s / elapsed, 3),
        "pg_active_max": max(act), "pg_active_avg": round(sum(act) / len(act), 1),
        "pg_conn_total_max": max(tot),
    }
    tx = s.get("transactions") or 0
    row["pod_cpu_ms_per_req"] = round(1000 * pod_cpu_s / tx, 2) if tx else None
    row["pg_cpu_ms_per_req"] = round(1000 * pg_cpu_s / tx, 2) if tx else None
    print(json.dumps(row, ensure_ascii=False), flush=True)
    out.write(json.dumps(row, ensure_ascii=False) + "\n")
    out.flush()
    time.sleep(5)


def warmup():
    for name, target in SCENARIOS.items():
        siege(target, 8, "10S")


# ---------------------------------------------------------------- fases
def phase_a(out):
    """Curva de saturação: 1 réplica, concorrência crescente, todos os cenários."""
    set_replicas(1)
    warmup()
    for scenario in SCENARIOS:
        for c in (1, 8, 32, 64):
            run_one("A", scenario, c, "20S", 1, out)


def restart_pods(n):
    set_replicas(n)
    sh(["kubectl", "-n", NS, "rollout", "restart", f"deploy/{DEPLOY}"])
    sh(["kubectl", "-n", NS, "rollout", "status", f"deploy/{DEPLOY}", "--timeout=300s"])
    while len(pods()) != n:
        time.sleep(2)
    time.sleep(5)


def phase_a2(out):
    """Cenários de geometria grande isolados: pod reiniciado antes de cada rodada,
    para que o acúmulo de requisições de uma rodada não contamine a seguinte."""
    for scenario in ("S6-mg-grande", "S7-intersec-grande"):
        for c in (1, 8, 32, 64):
            restart_pods(1)
            siege(SCENARIOS[scenario], 1, "5S")  # aquecimento leve
            run_one("A2", scenario, c, "20S", 1, out)


def phase_b(out):
    """Escalabilidade horizontal: réplicas fixas 1, 2, 4; pods reiniciados a cada rodada.
    Concorrência por cenário escolhida acima da saturação de 1 pod e (para S7)
    abaixo do ponto de OutOfMemoryError observado na fase A2."""
    plan = (("S2-listar", 32), ("S4-mg-pequena", 64),
            ("S5-intersec-media", 64), ("S7-intersec-grande", 8))
    for r in (1, 2, 4):
        for scenario, c in plan:
            restart_pods(r)
            siege(SCENARIOS[scenario], 8, "10S")  # aquecimento (JIT)
            run_one("B", scenario, c, "30S", r, out)


def phase_c(out):
    """Autoscaling (HPA 1..5, alvo 70% CPU) sob carga contínua."""
    restart_pods(1)
    sh(["kubectl", "apply", "-f", str(BASE.parent / "k8s" / "04-hpa.yaml")])
    time.sleep(60)  # metrics-server precisa de amostras antes da carga
    timeline = []
    stop = threading.Event()

    def watch():
        t0 = time.time()
        while not stop.is_set():
            hpa = json.loads(sh(["kubectl", "-n", NS, "get", "hpa", DEPLOY, "-o", "json"]))
            st = hpa.get("status", {})
            cpu = next((m["resource"]["current"].get("averageUtilization")
                        for m in st.get("currentMetrics") or [] if m.get("resource")), None)
            ready = sh(["kubectl", "-n", NS, "get", "deploy", DEPLOY, "-o",
                        "jsonpath={.status.readyReplicas}"]) or "0"
            timeline.append({"t": round(time.time() - t0), "cpu_pct": cpu,
                             "desired": st.get("desiredReplicas"),
                             "ready": int(ready)})
            stop.wait(10)

    th = threading.Thread(target=watch, daemon=True)
    th.start()
    # 4 minutos de carga contínua, em janelas de 30 s para ter a série temporal
    # cenário limitado por CPU do pod (S4), onde o HPA tem como ajudar
    for _ in range(8):
        run_one("C", "S4-mg-pequena", 64, "30S", -1, out)
    time.sleep(90)  # observa o pós-carga
    stop.set()
    th.join()
    (RESULTS / "C-timeline.json").write_text(json.dumps(timeline, indent=1))


if __name__ == "__main__":
    RESULTS.mkdir(exist_ok=True)
    for ph in sys.argv[1:]:
        with open(RESULTS / f"{ph}.jsonl", "w") as out:
            {"A": phase_a, "A2": phase_a2, "B": phase_b, "C": phase_c}[ph](out)
