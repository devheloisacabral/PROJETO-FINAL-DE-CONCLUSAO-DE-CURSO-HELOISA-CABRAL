import json
import tempfile
import unittest
from pathlib import Path

from compare_branches import BranchSnapshot, metric_cell, parse_selection, write_reports


class ComparisonTest(unittest.TestCase):
    def test_selection_preserves_explicit_order_and_removes_duplicates(self):
        available = [{"name": "prompt-01"}, {"name": "prompt-02"}]
        self.assertEqual(parse_selection("prompt-02, prompt-01, prompt-02", available), ["prompt-02", "prompt-01"])

    def test_all_uses_natural_order(self):
        available = [{"name": "prompt-10"}, {"name": "prompt-2"}, {"name": "prompt-1"}]
        self.assertEqual(parse_selection("ALL", available), ["prompt-1", "prompt-2", "prompt-10"])

    def test_metric_cell_marks_direction(self):
        self.assertIn("🟢", metric_cell(85, 80, True))
        self.assertIn("🔴", metric_cell(4, 2, False))

    def test_reports_include_delta_and_machine_readable_data(self):
        measures_a = {key: None for key in __import__("compare_branches").METRICS}
        measures_b = dict(measures_a)
        measures_a.update(coverage=70.0, bugs=2.0)
        measures_b.update(coverage=80.0, bugs=1.0)
        snapshots = [
            BranchSnapshot("prompt-01", "2026-01-01", "OK", measures_a),
            BranchSnapshot("prompt-02", "2026-01-02", "ERROR", measures_b),
        ]
        with tempfile.TemporaryDirectory() as directory:
            write_reports(snapshots, None, Path(directory), "project", "https://sonarcloud.io")
            markdown = (Path(directory) / "impacto-branches.md").read_text()
            payload = json.loads((Path(directory) / "impacto-branches.json").read_text())
            self.assertIn("80 (🟢 +10.00)", markdown)
            self.assertIn("1 (🟢 -1.00)", markdown)
            self.assertEqual(payload["branches"][1]["name"], "prompt-02")


if __name__ == "__main__":
    unittest.main()
