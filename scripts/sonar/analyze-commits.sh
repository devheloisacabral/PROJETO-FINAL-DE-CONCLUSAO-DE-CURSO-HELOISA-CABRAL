#!/usr/bin/env bash
set -Eeuo pipefail

: "${SONAR_HOST_URL:?Configure a variável SONAR_HOST_URL no repositório}"
: "${SONAR_TOKEN:?Configure o secret SONAR_TOKEN no repositório}"

project_key="${SONAR_PROJECT_KEY:-${GITHUB_REPOSITORY//\//_}}"
after_commit="${PUSH_AFTER:-${GITHUB_SHA:?GITHUB_SHA não está disponível}}"
before_commit="${PUSH_BEFORE:-}"
event_name="${PUSH_EVENT:-push}"
reports_dir="${GITHUB_WORKSPACE:-.}/sonar/reports"
metrics="bugs,vulnerabilities,security_hotspots,code_smells,coverage,duplicated_lines_density,complexity,ncloc"
mkdir -p "$reports_dir"
rm -f "$reports_dir/impacto-commits.md" "$reports_dir/commits.csv"

if [[ "$event_name" == "workflow_dispatch" ]]; then
  commits=("$after_commit")
elif [[ -z "$before_commit" || "$before_commit" =~ ^0+$ ]]; then
  mapfile -t commits < <(git rev-list --reverse "$after_commit")
else
  mapfile -t commits < <(git rev-list --reverse "$before_commit..$after_commit")
fi

if [[ ${#commits[@]} -eq 0 ]]; then
  commits=("$after_commit")
fi

json_value() {
  local payload="$1"
  local metric="$2"
  jq -r --arg metric "$metric" '.component.measures[]? | select(.metric == $metric) | .value' <<< "$payload" | head -n 1
}

number_or_na() {
  if [[ -n "${1:-}" && "$1" != "null" ]]; then
    printf '%s' "$1"
  else
    printf 'N/A'
  fi
}

delta() {
  local current="${1:-N/A}"
  local previous="${2:-N/A}"
  if [[ "$current" == "N/A" || "$previous" == "N/A" ]]; then
    printf 'N/A'
    return
  fi
  awk -v current="$current" -v previous="$previous" 'BEGIN { printf "%.2f", current - previous }'
}

analysis_metrics='{}'
run_analysis() {
  local commit="$1"
  local version="$2"
  local report_task
  local task_id
  local analysis_id
  local task_status
  local measures

  git checkout --detach --force "$commit" >/dev/null
  rm -rf target
  mkdir -p "$reports_dir"

  if [[ -f ./mvnw ]]; then
    mvn_command=(bash ./mvnw)
  else
    mvn_command=(mvn)
  fi

  sonar_arguments=(
    "-Dsonar.projectKey=$project_key"
    "-Dsonar.projectVersion=$version"
    "-Dsonar.scm.revision=$commit"
    "-Dsonar.host.url=$SONAR_HOST_URL"
  )
  if [[ -n "${SONAR_ORGANIZATION:-}" ]]; then
    sonar_arguments+=("-Dsonar.organization=$SONAR_ORGANIZATION")
  fi

  "${mvn_command[@]}" -B -DskipTests verify \
    org.sonarsource.scanner.maven:sonar-maven-plugin:5.2.0.4988:sonar \
    "${sonar_arguments[@]}"

  report_task="target/sonar/report-task.txt"
  [[ -f "$report_task" ]] || { echo "report-task.txt não foi gerado para $commit" >&2; return 1; }
  task_id="$(sed -n 's/^ceTaskId=//p' "$report_task")"
  [[ -n "$task_id" ]] || { echo "ceTaskId ausente para $commit" >&2; return 1; }

  for _ in {1..60}; do
    task_status="$(curl --fail --silent --show-error \
      --header "Authorization: Bearer $SONAR_TOKEN" \
      "$SONAR_HOST_URL/api/ce/task?id=$task_id")"
    case "$(jq -r '.task.status' <<< "$task_status")" in
      SUCCESS)
        analysis_id="$(jq -r '.task.analysisId' <<< "$task_status")"
        break
        ;;
      FAILED|CANCELED)
        jq -r '.task.errorMessage // "Análise SonarQube falhou"' <<< "$task_status" >&2
        return 1
        ;;
    esac
    sleep 5
  done

  [[ -n "${analysis_id:-}" && "$analysis_id" != "null" ]] || {
    echo "A análise SonarQube não terminou para $commit" >&2
    return 1
  }

  measures="$(curl --fail --silent --show-error \
    --header "Authorization: Bearer $SONAR_TOKEN" \
    --get "$SONAR_HOST_URL/api/measures/component" \
    --data-urlencode "component=$project_key" \
    --data-urlencode "metricKeys=$metrics")"
  analysis_metrics="$measures"
}

previous_commit=""
previous_metrics='{}'
if [[ ${#commits[@]} -gt 0 ]]; then
  first_commit="${commits[0]}"
  if parent_commit="$(git rev-parse "${first_commit}^" 2>/dev/null)" && git cat-file -e "$parent_commit^{commit}" 2>/dev/null; then
    git show "$parent_commit:pom.xml" >/dev/null 2>&1 && run_analysis "$parent_commit" "baseline-$parent_commit"
    if [[ -n "${analysis_metrics:-}" && "$analysis_metrics" != '{}' ]]; then
      previous_commit="$parent_commit"
      previous_metrics="$analysis_metrics"
    fi
  fi
fi

{
  echo 'commit,previous_commit,date,bugs,vulnerabilities,security_hotspots,code_smells,coverage,duplicated_lines_density,complexity,ncloc'
} > "$reports_dir/commits.csv"

{
  echo '# Histórico de qualidade por commit'
  echo
  echo "Projeto SonarQube: \`$project_key\`"
  echo
  echo '| Commit | Commit anterior | Bugs | Vulnerabilidades | Hotspots | Code smells | Cobertura | Duplicação | Complexidade | Linhas |'
  echo '|---|---|---:|---:|---:|---:|---:|---:|---:|---:|'
} > "$reports_dir/impacto-commits.md"

for commit in "${commits[@]}"; do
  if ! git cat-file -e "$commit:pom.xml" 2>/dev/null; then
    echo "- \`$commit\`: ignorado porque não contém pom.xml." >> "$reports_dir/impacto-commits.md"
    continue
  fi
  run_analysis "$commit" "$commit"
  date="$(git show -s --format=%cI "$commit")"
  parent="$(git rev-parse "$commit^" 2>/dev/null || printf '%s' '-')"

  bugs="$(number_or_na "$(json_value "$analysis_metrics" bugs)")"
  vulnerabilities="$(number_or_na "$(json_value "$analysis_metrics" vulnerabilities)")"
  hotspots="$(number_or_na "$(json_value "$analysis_metrics" security_hotspots)")"
  smells="$(number_or_na "$(json_value "$analysis_metrics" code_smells)")"
  coverage="$(number_or_na "$(json_value "$analysis_metrics" coverage)")"
  duplication="$(number_or_na "$(json_value "$analysis_metrics" duplicated_lines_density)")"
  complexity="$(number_or_na "$(json_value "$analysis_metrics" complexity)")"
  ncloc="$(number_or_na "$(json_value "$analysis_metrics" ncloc)")"

  previous_bugs="$(number_or_na "$(json_value "$previous_metrics" bugs)")"
  previous_vulnerabilities="$(number_or_na "$(json_value "$previous_metrics" vulnerabilities)")"
  previous_hotspots="$(number_or_na "$(json_value "$previous_metrics" security_hotspots)")"
  previous_smells="$(number_or_na "$(json_value "$previous_metrics" code_smells)")"
  previous_coverage="$(number_or_na "$(json_value "$previous_metrics" coverage)")"
  previous_duplication="$(number_or_na "$(json_value "$previous_metrics" duplicated_lines_density)")"
  previous_complexity="$(number_or_na "$(json_value "$previous_metrics" complexity)")"
  previous_ncloc="$(number_or_na "$(json_value "$previous_metrics" ncloc)")"

  printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
    "$commit" "${previous_commit:--}" "$date" "$bugs" "$vulnerabilities" "$hotspots" "$smells" "$coverage" "$duplication" "$complexity" "$ncloc" \
    >> "$reports_dir/commits.csv"
  printf '| `%s` | `%s` | %s (%s) | %s (%s) | %s (%s) | %s (%s) | %s (%s) | %s (%s) | %s (%s) | %s (%s) |\n' \
    "${commit:0:12}" "${previous_commit:0:12}" "$bugs" "$(delta "$bugs" "$previous_bugs")" "$vulnerabilities" "$(delta "$vulnerabilities" "$previous_vulnerabilities")" "$hotspots" "$(delta "$hotspots" "$previous_hotspots")" "$smells" "$(delta "$smells" "$previous_smells")" "$coverage" "$(delta "$coverage" "$previous_coverage")" "$duplication" "$(delta "$duplication" "$previous_duplication")" "$complexity" "$(delta "$complexity" "$previous_complexity")" "$ncloc" "$(delta "$ncloc" "$previous_ncloc")" \
    >> "$reports_dir/impacto-commits.md"

  previous_commit="$commit"
  previous_metrics="$analysis_metrics"
done

git checkout --detach --force "$after_commit" >/dev/null
