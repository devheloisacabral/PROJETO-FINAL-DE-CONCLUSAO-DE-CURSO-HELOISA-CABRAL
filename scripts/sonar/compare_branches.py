#!/usr/bin/env python3
"""Compara snapshots já processados de branches de um projeto SonarCloud."""

from __future__ import annotations

import base64
import csv
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

METRICS = (
    "bugs",
    "vulnerabilities",
    "security_hotspots",
    "code_smells",
    "coverage",
    "line_coverage",
    "branch_coverage",
    "duplicated_lines_density",
    "duplicated_blocks",
    "complexity",
    "cognitive_complexity",
    "ncloc",
    "tests",
    "test_success_density",
)

DISPLAY_METRICS = (
    ("bugs", "Bugs", False),
    ("vulnerabilities", "Vulnerabilidades", False),
    ("security_hotspots", "Hotspots", False),
    ("code_smells", "Code smells", False),
    ("coverage", "Cobertura (%)", True),
    ("branch_coverage", "Cobertura de branches (%)", True),
    ("duplicated_lines_density", "Duplicação (%)", False),
    ("complexity", "Complexidade", False),
    ("cognitive_complexity", "Complexidade cognitiva", False),
    ("ncloc", "Linhas de código", False),
)


class ComparisonError(RuntimeError):
    """Erro que deve ser exibido ao usuário sem traceback."""


@dataclass(frozen=True)
class BranchSnapshot:
    name: str
    analysis_date: str
    quality_gate: str
    measures: dict[str, float | None]


class SonarClient:
    def __init__(self, host: str, token: str, project: str) -> None:
        self.host = host.rstrip("/")
        self.project = project
        credentials = base64.b64encode(f"{token}:".encode()).decode()
        self.headers = {"Authorization": f"Basic {credentials}", "Accept": "application/json"}

    def get(self, endpoint: str, **params: str) -> dict[str, Any]:
        url = f"{self.host}{endpoint}?{urllib.parse.urlencode(params)}"
        request = urllib.request.Request(url, headers=self.headers)
        for attempt in range(4):
            try:
                with urllib.request.urlopen(request, timeout=30) as response:
                    return json.load(response)
            except urllib.error.HTTPError as error:
                detail = error.read().decode(errors="replace")
                if error.code not in (429, 502, 503, 504) or attempt == 3:
                    raise ComparisonError(f"SonarCloud respondeu HTTP {error.code}: {detail}") from error
            except urllib.error.URLError as error:
                if attempt == 3:
                    raise ComparisonError(f"Não foi possível acessar o SonarCloud: {error.reason}") from error
            time.sleep(2**attempt)
        raise AssertionError("tentativas esgotadas")

    def list_branches(self) -> list[dict[str, Any]]:
        payload = self.get("/api/project_branches/list", project=self.project)
        return payload.get("branches", [])

    def snapshot(self, branch: str, branch_info: dict[str, Any]) -> BranchSnapshot:
        payload = self.get(
            "/api/measures/component",
            component=self.project,
            branch=branch,
            metricKeys=",".join(METRICS),
        )
        values: dict[str, float | None] = {metric: None for metric in METRICS}
        for measure in payload.get("component", {}).get("measures", []):
            try:
                values[measure["metric"]] = float(measure["value"])
            except (KeyError, TypeError, ValueError):
                continue
        status = branch_info.get("status", {}).get("qualityGateStatus", "NONE")
        return BranchSnapshot(branch, branch_info.get("analysisDate", ""), status, values)


def parse_selection(raw: str, available: list[dict[str, Any]]) -> list[str]:
    names = [item["name"] for item in available if item.get("name")]
    if raw.strip().upper() == "ALL":
        # O nome natural do experimento (prompt-01, prompt-02...) ordena corretamente.
        selected = sorted(names, key=natural_key)
    else:
        selected = [part.strip() for part in re.split(r"[,\n;]+", raw) if part.strip()]
    selected = list(dict.fromkeys(selected))
    if not selected:
        raise ComparisonError("Informe ao menos uma branch.")
    if len(selected) > 10:
        raise ComparisonError(f"Foram selecionadas {len(selected)} branches; o limite do experimento é 10.")
    missing = [name for name in selected if name not in names]
    if missing:
        raise ComparisonError("Branches sem análise no SonarCloud: " + ", ".join(missing))
    return selected


def natural_key(value: str) -> list[int | str]:
    return [int(part) if part.isdigit() else part.casefold() for part in re.split(r"(\d+)", value)]


def number(value: float | None) -> str:
    if value is None:
        return "N/D"
    return f"{value:.2f}".rstrip("0").rstrip(".")


def metric_cell(current: float | None, previous: float | None, higher_is_better: bool) -> str:
    if current is None:
        return "N/D"
    if previous is None:
        return number(current)
    delta = current - previous
    if abs(delta) < 0.005:
        marker = "="
    elif (delta > 0) == higher_is_better:
        marker = "🟢"
    else:
        marker = "🔴"
    return f"{number(current)} ({marker} {delta:+.2f})"


def write_reports(snapshots: list[BranchSnapshot], baseline: BranchSnapshot | None, output: Path, project: str, host: str) -> None:
    output.mkdir(parents=True, exist_ok=True)
    generated = datetime.now(timezone.utc).isoformat()
    all_snapshots = ([baseline] if baseline else []) + snapshots
    assert all(item is not None for item in all_snapshots)

    json_data = {
        "project": project,
        "generatedAt": generated,
        "baseline": baseline.name if baseline else None,
        "branches": [snapshot.__dict__ for snapshot in snapshots],
    }
    (output / "impacto-branches.json").write_text(json.dumps(json_data, indent=2, ensure_ascii=False) + "\n")

    fields = ["ordem", "branch", "comparada_com", "analysis_date", "quality_gate"]
    fields += [item for metric in METRICS for item in (metric, f"delta_{metric}")]
    with (output / "impacto-branches.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        previous = baseline
        for index, snapshot in enumerate(snapshots, 1):
            row: dict[str, Any] = {
                "ordem": index,
                "branch": snapshot.name,
                "comparada_com": previous.name if previous else "",
                "analysis_date": snapshot.analysis_date,
                "quality_gate": snapshot.quality_gate,
            }
            for metric in METRICS:
                value = snapshot.measures[metric]
                old = previous.measures[metric] if previous else None
                row[metric] = "" if value is None else number(value)
                row[f"delta_{metric}"] = "" if value is None or old is None else f"{value - old:.2f}"
            writer.writerow(row)
            previous = snapshot

    headings = ["# Impacto das branches no SonarCloud", "", f"Projeto: [`{project}`]({host}/dashboard?id={urllib.parse.quote(project)})", ""]
    headings += ["> Cada valor entre parênteses é o delta em relação à linha anterior. 🟢 indica melhora, 🔴 indica piora e = indica estabilidade.", ""]
    columns = ["#", "Branch", "Comparada com", "Quality Gate"] + [label for _, label, _ in DISPLAY_METRICS]
    lines = headings + ["| " + " | ".join(columns) + " |", "|" + "|".join(["---"] * len(columns)) + "|"]
    previous = baseline
    for index, snapshot in enumerate(snapshots, 1):
        branch_url = f"{host}/dashboard?id={urllib.parse.quote(project)}&branch={urllib.parse.quote(snapshot.name)}"
        gate = {"OK": "✅ OK", "ERROR": "❌ ERROR"}.get(snapshot.quality_gate, f"⚪ {snapshot.quality_gate}")
        row = [str(index), f"[{snapshot.name}]({branch_url})", previous.name if previous else "—", gate]
        row += [metric_cell(snapshot.measures[key], previous.measures[key] if previous else None, good) for key, _, good in DISPLAY_METRICS]
        lines.append("| " + " | ".join(row) + " |")
        previous = snapshot
    lines += ["", "## Metodologia", "", "* As métricas são snapshots já processados pelo SonarCloud; este workflow não altera nem reanalisa as branches.", "* A ordem informada determina as comparações: cada linha é subtraída da anterior.", "* O CSV contém todas as métricas e deltas, inclusive cobertura de linhas, blocos duplicados e testes.", "", f"Gerado em `{generated}`."]
    (output / "impacto-branches.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    try:
        host = os.getenv("SONAR_HOST_URL", "https://sonarcloud.io")
        project = os.environ["SONAR_PROJECT_KEY"]
        token = os.environ["SONAR_TOKEN"]
        raw_selection = os.getenv("SELECTED_BRANCHES", "ALL")
        baseline_name = os.getenv("BASELINE_BRANCH", "").strip()
        if not token:
            raise ComparisonError("O secret SONAR_TOKEN não foi configurado.")
        client = SonarClient(host, token, project)
        available = client.list_branches()
        selected = parse_selection(raw_selection, available)
        info = {branch["name"]: branch for branch in available}
        if baseline_name and baseline_name not in info:
            raise ComparisonError(f"A baseline '{baseline_name}' não possui análise no SonarCloud.")
        if baseline_name in selected:
            raise ComparisonError("A baseline não deve ser repetida na lista de branches.")
        baseline = client.snapshot(baseline_name, info[baseline_name]) if baseline_name else None
        snapshots = [client.snapshot(name, info[name]) for name in selected]
        output = Path(os.getenv("REPORTS_DIR", "sonar/reports"))
        write_reports(snapshots, baseline, output, project, host)
        failed = [item.name for item in snapshots if item.quality_gate == "ERROR"]
        print(f"Relatórios gerados em {output} para {len(snapshots)} branch(es).")
        if failed and os.getenv("FAIL_ON_QUALITY_GATE", "false").lower() == "true":
            raise ComparisonError("Quality Gate reprovado: " + ", ".join(failed))
        return 0
    except KeyError as error:
        print(f"Variável obrigatória ausente: {error.args[0]}", file=sys.stderr)
    except ComparisonError as error:
        print(f"Erro: {error}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
