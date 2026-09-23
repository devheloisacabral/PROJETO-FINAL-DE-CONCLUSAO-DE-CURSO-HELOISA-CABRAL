#!/usr/bin/env python3
"""Analisa branches como projetos SonarCloud independentes e compara as métricas."""

from __future__ import annotations

import base64
import csv
import hashlib
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

METRICS = (
    "bugs", "vulnerabilities", "security_hotspots", "code_smells", "coverage",
    "line_coverage", "branch_coverage", "duplicated_lines_density", "duplicated_blocks",
    "complexity", "cognitive_complexity", "ncloc", "tests", "test_success_density",
    "test_failures", "test_errors", "skipped_tests", "test_execution_time", "sqale_index",
)
EXPERIMENTAL_BRANCH = re.compile(r"^(?:prompt|manutencao)-\d+(?:-|$)", re.IGNORECASE)
DISPLAY_METRICS = (
    ("bugs", "Bugs", False), ("vulnerabilities", "Vulnerabilidades", False),
    ("security_hotspots", "Hotspots", False), ("code_smells", "Code smells", False),
    ("coverage", "Cobertura (%)", True), ("branch_coverage", "Cobertura branches (%)", True),
    ("duplicated_lines_density", "Duplicação (%)", False), ("complexity", "Complexidade", False),
    ("cognitive_complexity", "Complexidade cognitiva", False), ("ncloc", "Linhas", False),
    ("sqale_index", "Débito técnico (min)", False), ("tests", "Testes", True),
    ("test_failures", "Falhas de testes", False), ("test_errors", "Erros de testes", False),
    ("skipped_tests", "Testes ignorados", False), ("test_success_density", "Sucesso dos testes (%)", True),
)


class ComparisonError(RuntimeError):
    """Erro que deve ser exibido ao usuário sem traceback."""


@dataclass(frozen=True)
class BranchSnapshot:
    name: str
    project_key: str
    commit: str
    analysis_date: str
    quality_gate: str
    measures: dict[str, float | None]


class SonarClient:
    def __init__(self, host: str, token: str, organization: str) -> None:
        self.host = host.rstrip("/")
        self.organization = organization
        credentials = base64.b64encode(f"{token}:".encode()).decode()
        self.headers = {"Authorization": f"Basic {credentials}", "Accept": "application/json"}

    def request(self, method: str, endpoint: str, **params: str) -> dict[str, Any]:
        encoded = urllib.parse.urlencode(params)
        url = f"{self.host}{endpoint}" + (f"?{encoded}" if method == "GET" else "")
        data = encoded.encode() if method == "POST" else None
        request = urllib.request.Request(url, data=data, headers=self.headers, method=method)
        for attempt in range(4):
            try:
                with urllib.request.urlopen(request, timeout=30) as response:
                    body = response.read()
                    return json.loads(body) if body else {}
            except urllib.error.HTTPError as error:
                detail = error.read().decode(errors="replace")
                if error.code not in (429, 502, 503, 504) or attempt == 3:
                    hint = ""
                    if error.code == 403:
                        hint = " Verifique se SONAR_TOKEN possui a permissão 'Create Projects' na organização."
                    raise ComparisonError(f"SonarCloud respondeu HTTP {error.code}: {detail}{hint}") from error
            except urllib.error.URLError as error:
                if attempt == 3:
                    raise ComparisonError(f"Não foi possível acessar o SonarCloud: {error.reason}") from error
            time.sleep(2**attempt)
        raise AssertionError("tentativas esgotadas")

    def get(self, endpoint: str, **params: str) -> dict[str, Any]:
        return self.request("GET", endpoint, **params)

    def post(self, endpoint: str, **params: str) -> dict[str, Any]:
        return self.request("POST", endpoint, **params)

    def ensure_project(self, key: str, name: str, main_branch: str) -> None:
        try:
            self.get("/api/components/show", component=key)
            print(f"Projeto SonarCloud já existe: {key}")
            return
        except ComparisonError as error:
            if "HTTP 404" not in str(error):
                raise
        self.post("/api/projects/create", organization=self.organization, project=key, name=name)
        self.post("/api/project_branches/rename", project=key, name=main_branch)
        print(f"Projeto SonarCloud criado: {key}")

    def wait_for_analysis(self, task_id: str) -> dict[str, Any]:
        for _ in range(90):
            task = self.get("/api/ce/task", id=task_id).get("task", {})
            status = task.get("status")
            if status == "SUCCESS":
                return task
            if status in ("FAILED", "CANCELED"):
                raise ComparisonError(task.get("errorMessage", f"Análise SonarCloud {status}"))
            time.sleep(4)
        raise ComparisonError("A análise não terminou no prazo de 6 minutos.")

    def official_analysis_date(self, project_key: str, task: dict[str, Any]) -> str:
        """Obtém a data oficial da análise, sem depender do relógio do runner."""
        for field in ("executedAt", "submittedAt"):
            value = task.get(field)
            if value:
                return str(value)
        analyses = self.get("/api/project_analyses/search", project=project_key, ps="1").get("analyses", [])
        if analyses:
            return str(analyses[0].get("date") or analyses[0].get("analysisDate") or "N/D")
        return "N/D"

    def snapshot(self, branch: str, project_key: str, commit: str, analysis_date: str) -> BranchSnapshot:
        payload = self.get("/api/measures/component", component=project_key, metricKeys=",".join(METRICS))
        values: dict[str, float | None] = {metric: None for metric in METRICS}
        for measure in payload.get("component", {}).get("measures", []):
            try:
                values[measure["metric"]] = float(measure["value"])
            except (KeyError, TypeError, ValueError):
                continue
        gate = self.get("/api/qualitygates/project_status", projectKey=project_key)
        status = gate.get("projectStatus", {}).get("status", "NONE")
        return BranchSnapshot(branch, project_key, commit, analysis_date, status, values)


def run(*args: str, capture: bool = False, env: dict[str, str] | None = None) -> str:
    result = subprocess.run(args, check=False, text=True, capture_output=capture, env=env)
    if result.returncode:
        if capture and result.stderr:
            print(result.stderr, file=sys.stderr)
        raise ComparisonError(f"Comando falhou ({result.returncode}): {' '.join(args)}")
    return result.stdout.strip() if capture else ""


def available_branches() -> list[str]:
    output = run("git", "for-each-ref", "--format=%(refname:strip=3)", "refs/remotes/origin", capture=True)
    return [name for name in output.splitlines() if name and name != "HEAD"]


def natural_key(value: str) -> list[int | str]:
    return [int(part) if part.isdigit() else part.casefold() for part in re.split(r"(\d+)", value)]


def experiment_order_key(value: str) -> tuple[int, list[int | str]]:
    match = re.match(r"^(?:prompt|manutencao)-(\d+)(?:-|$)", value, re.IGNORECASE)
    if match:
        return int(match.group(1)), natural_key(value)
    return sys.maxsize, natural_key(value)


def parse_selection(raw: str, available: list[str]) -> list[str]:
    selected = sorted((branch for branch in available if EXPERIMENTAL_BRANCH.match(branch)), key=experiment_order_key) if raw.strip().upper() == "ALL" else [
        part.strip() for part in re.split(r"[,\n;]+", raw) if part.strip()
    ]
    selected = list(dict.fromkeys(selected))
    if not selected:
        raise ComparisonError("Informe ao menos uma branch.")
    if len(selected) > 20:
        raise ComparisonError(f"Foram selecionadas {len(selected)} branches; informe no máximo 20.")
    missing = [name for name in selected if name not in available]
    if missing:
        raise ComparisonError("Branches inexistentes no Git remoto: " + ", ".join(missing))
    return selected


def project_key(base: str, branch: str) -> str:
    slug = re.sub(r"[^A-Za-z0-9_.-]+", "-", branch).strip("-.")[:80] or "branch"
    digest = hashlib.sha256(branch.encode()).hexdigest()[:8]
    return f"{base}__experiment__{slug}-{digest}"[:400]


def analyze_branch(client: SonarClient, branch: str, base_project: str, token: str) -> BranchSnapshot:
    commit = run("git", "rev-parse", f"refs/remotes/origin/{branch}^{{commit}}", capture=True)
    # Todas as referências são analisadas sequencialmente no projeto principal.
    # Assim o SonarCloud mantém o histórico das análises sem consumir a cota de
    # LOC criando um projeto independente para cada branch.
    key = base_project
    run("git", "checkout", "--detach", "--force", commit)
    run("git", "clean", "-fdx", "--", "target")
    maven = ["bash", "./mvnw"] if Path("mvnw").is_file() else ["mvn"]
    # Cada branch experimental é um projeto SonarCloud independente. Não envie
    # sonar.branch.name: a organização não permite análise de branches secundárias.
    scan_env = os.environ.copy()
    for name in ("GITHUB_REF", "GITHUB_REF_NAME", "GITHUB_HEAD_REF", "GITHUB_BASE_REF", "GITHUB_EVENT_NAME"):
        scan_env.pop(name, None)
    command = maven + [
        "-B", "verify", "org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar",
        f"-Dsonar.projectKey={key}", f"-Dsonar.organization={client.organization}",
        f"-Dsonar.host.url={client.host}", f"-Dsonar.token={token}",
        f"-Dsonar.scm.revision={commit}", f"-Dsonar.projectVersion={commit[:12]}",
        "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml",
    ]
    run(*command, env=scan_env)
    report = Path("target/sonar/report-task.txt")
    if not report.is_file():
        raise ComparisonError(f"O scanner não gerou {report} para {branch}.")
    fields = dict(line.split("=", 1) for line in report.read_text().splitlines() if "=" in line)
    task_id = fields.get("ceTaskId")
    if not task_id:
        raise ComparisonError(f"ceTaskId ausente no relatório do scanner para {branch}.")
    task = client.wait_for_analysis(task_id)
    return client.snapshot(branch, key, commit, client.official_analysis_date(key, task))


def number(value: float | None) -> str:
    return "N/D" if value is None else f"{value:.2f}".rstrip("0").rstrip(".")


def metric_cell(current: float | None, previous: float | None, higher_is_better: bool) -> str:
    if current is None:
        return "N/D"
    if previous is None:
        return number(current)
    delta = current - previous
    marker = "=" if abs(delta) < 0.005 else ("🟢" if (delta > 0) == higher_is_better else "🔴")
    return f"{number(current)} ({marker} {delta:+.2f})"


def write_reports(snapshots: list[BranchSnapshot], baseline: BranchSnapshot | None, output: Path, base: str, host: str) -> None:
    output.mkdir(parents=True, exist_ok=True)
    generated = datetime.now(timezone.utc).isoformat()
    data = {"baseProject": base, "generatedAt": generated, "baseline": baseline.name if baseline else None,
            "branches": [asdict(snapshot) for snapshot in snapshots]}
    (output / "impacto-branches.json").write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")
    fields = ["ordem", "branch", "project_key", "commit", "comparada_com", "analysis_date", "quality_gate"]
    fields += [item for metric in METRICS for item in (metric, f"delta_{metric}")]
    with (output / "impacto-branches.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields); writer.writeheader(); previous = baseline
        for index, snapshot in enumerate(snapshots, 1):
            row: dict[str, Any] = {"ordem": index, "branch": snapshot.name, "project_key": snapshot.project_key,
                "commit": snapshot.commit, "comparada_com": previous.name if previous else "",
                "analysis_date": snapshot.analysis_date, "quality_gate": snapshot.quality_gate}
            for metric in METRICS:
                value, old = snapshot.measures[metric], previous.measures[metric] if previous else None
                row[metric] = "" if value is None else number(value)
                row[f"delta_{metric}"] = "" if value is None or old is None else f"{value - old:.2f}"
            writer.writerow(row); previous = snapshot
    columns = ["#", "Branch / projeto", "Comparada com", "Quality Gate"] + [label for _, label, _ in DISPLAY_METRICS]
    lines = ["# Impacto das branches no SonarCloud", "", "> Cada branch foi analisada como o projeto principal de um projeto SonarCloud independente.",
        "> O valor entre parênteses é o delta contra a linha anterior: 🟢 melhora, 🔴 piora e = estabilidade.", "",
        "| " + " | ".join(columns) + " |", "|" + "|".join(["---"] * len(columns)) + "|"]
    previous = baseline
    for index, snapshot in enumerate(snapshots, 1):
        url = f"{host}/dashboard?id={urllib.parse.quote(snapshot.project_key)}"
        gate = {"OK": "✅ OK", "ERROR": "❌ ERROR"}.get(snapshot.quality_gate, f"⚪ {snapshot.quality_gate}")
        row = [str(index), f"[{snapshot.name}]({url})<br><sub>{snapshot.project_key}</sub>", previous.name if previous else "—", gate]
        row += [metric_cell(snapshot.measures[key], previous.measures[key] if previous else None, good) for key, _, good in DISPLAY_METRICS]
        lines.append("| " + " | ".join(row) + " |"); previous = snapshot
    lines += ["", f"Gerado em `{generated}`."]
    (output / "impacto-branches.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    original = run("git", "rev-parse", "HEAD", capture=True)
    try:
        host = os.getenv("SONAR_HOST_URL", "https://sonarcloud.io")
        base, organization, token = os.environ["SONAR_PROJECT_KEY"], os.environ["SONAR_ORGANIZATION"], os.environ["SONAR_TOKEN"]
        if not token:
            raise ComparisonError("O secret SONAR_TOKEN não foi configurado.")
        available = available_branches()
        selected = parse_selection(os.getenv("SELECTED_BRANCHES", "ALL"), available)
        baseline_name = os.getenv("BASELINE_BRANCH", "").strip()
        if baseline_name and baseline_name not in available:
            raise ComparisonError(f"A baseline '{baseline_name}' não existe no Git remoto.")
        if baseline_name in selected:
            raise ComparisonError("A baseline não deve ser repetida na lista de branches.")
        client = SonarClient(host, token, organization)
        client.ensure_project(base, base, "main")
        baseline = analyze_branch(client, baseline_name, base, token) if baseline_name else None
        snapshots = [analyze_branch(client, branch, base, token) for branch in selected]
        write_reports(snapshots, baseline, Path(os.getenv("REPORTS_DIR", "sonar/reports")), base, host)
        failed = [snapshot.name for snapshot in snapshots if snapshot.quality_gate == "ERROR"]
        if failed and os.getenv("FAIL_ON_QUALITY_GATE", "false").lower() == "true":
            raise ComparisonError("Quality Gate reprovado: " + ", ".join(failed))
        return 0
    except KeyError as error:
        print(f"Variável obrigatória ausente: {error.args[0]}", file=sys.stderr)
    except ComparisonError as error:
        print(f"Erro: {error}", file=sys.stderr)
    finally:
        subprocess.run(("git", "checkout", "--detach", "--force", original), check=False)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
