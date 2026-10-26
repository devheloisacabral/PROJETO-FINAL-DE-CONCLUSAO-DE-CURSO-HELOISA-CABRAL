import json
import tempfile
import unittest
from pathlib import Path

from compare_branches import METRICS, BranchSnapshot, metric_cell, parse_selection, project_key, write_reports


class ComparisonTest(unittest.TestCase):
    def test_selection_preserves_explicit_order_and_removes_duplicates(self):
        available = ["prompt-01", "prompt-02"]
        self.assertEqual(parse_selection("prompt-02, prompt-01, prompt-02", available), ["prompt-02", "prompt-01"])

    def test_all_uses_natural_order(self):
        available = ["prompt-10", "prompt-2", "prompt-1", "codex/auxiliar", "resultado-10-prompts"]
        self.assertEqual(parse_selection("ALL", available), ["prompt-1", "prompt-2", "prompt-10"])

    def test_all_includes_maintenance_prompt_branches(self):
        available = [
            "manutencao-11-vencimento",
            "manutencao-19-conflito",
            "prompt-01-fundacao",
            "prompt-10-dashboard",
            "fix-01",
        ]
        self.assertEqual(
            parse_selection("ALL", available),
            [
                "prompt-01-fundacao",
                "prompt-10-dashboard",
                "manutencao-11-vencimento",
                "manutencao-19-conflito",
            ],
        )

    def test_project_key_is_safe_deterministic_and_collision_resistant(self):
        first = project_key("base", "feature/foo")
        self.assertEqual(first, project_key("base", "feature/foo"))
        self.assertRegex(first, r"^base__experiment__feature-foo-[0-9a-f]{8}$")
        self.assertNotEqual(first, project_key("base", "feature-foo"))

    def test_all_snapshots_can_share_the_main_project(self):
        first = BranchSnapshot("prompt-01", "base", "a" * 40, "2026-01-01", "OK", {})
        second = BranchSnapshot("prompt-02", "base", "b" * 40, "2026-01-02", "OK", {})
        self.assertEqual(first.project_key, second.project_key)

    def test_all_orders_prompt_and_maintenance_by_increment_number(self):
        available = [
            {"name": "manutencao-11-vencimento-individual-por-aluno"},
            {"name": "prompt-02-autenticacao"},
            {"name": "manutencao-20-reagendamento-de-aula"},
            {"name": "prompt-01-fundacao"},
        ]
        self.assertEqual(
            parse_selection("ALL", available),
            [
                "prompt-01-fundacao",
                "prompt-02-autenticacao",
                "manutencao-11-vencimento-individual-por-aluno",
                "manutencao-20-reagendamento-de-aula",
            ],
        )

    def test_metric_cell_marks_direction(self):
        self.assertIn("🟢", metric_cell(85, 80, True))
        self.assertIn("🔴", metric_cell(4, 2, False))

    def test_reports_include_projects_delta_and_machine_readable_data(self):
        measures_a = {key: None for key in METRICS}
        measures_b = dict(measures_a)
        measures_a.update(coverage=70.0, bugs=2.0)
        measures_b.update(coverage=80.0, bugs=1.0)
        snapshots = [
            BranchSnapshot("prompt-01", "project-01", "a" * 40, "2026-01-01", "OK", measures_a),
            BranchSnapshot("prompt-02", "project-02", "b" * 40, "2026-01-02", "ERROR", measures_b),
        ]
        with tempfile.TemporaryDirectory() as directory:
            write_reports(snapshots, None, Path(directory), "base", "https://sonarcloud.io")
            markdown = (Path(directory) / "impacto-branches.md").read_text(encoding="utf-8")
            payload = json.loads((Path(directory) / "impacto-branches.json").read_text())
            self.assertIn("80 (🟢 +10.00)", markdown)
            self.assertIn("1 (🟢 -1.00)", markdown)
            self.assertIn("project-02", markdown)
            self.assertEqual(payload["branches"][1]["project_key"], "project-02")

    def test_collects_debt_and_test_detail_metrics(self):
        for metric in ("sqale_index", "test_failures", "test_errors", "skipped_tests", "test_execution_time"):
            self.assertIn(metric, METRICS)


if __name__ == "__main__":
    unittest.main()
