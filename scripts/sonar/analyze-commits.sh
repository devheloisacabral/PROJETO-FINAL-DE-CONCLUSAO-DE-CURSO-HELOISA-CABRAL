#!/usr/bin/env bash
set -Eeuo pipefail

: "${SONAR_HOST_URL:?Configure a variável SONAR_HOST_URL no repositório}"
: "${SONAR_TOKEN:?Configure o secret SONAR_TOKEN no repositório}"

project_key="${SONAR_PROJECT_KEY:-${GITHUB_REPOSITORY//\//_}}"
head_commit="${PUSH_AFTER:-${GITHUB_SHA:?GITHUB_SHA não está disponível}}"
current_branch="${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-}}"
base_branch="${SONAR_BASE_BRANCH:-${GITHUB_BASE_REF:-}}"
reports_dir="${GITHUB_WORKSPACE:-.}/sonar/reports"
metrics="bugs,vulnerabilities,security_hotspots,code_smells,coverage,duplicated_lines_density,complexity,ncloc"

mkdir -p "$reports_dir"
rm -f "$reports_dir/impacto-branches.md" "$reports_dir/branches.csv"

if [[ -z "$current_branch" ]]; then
  current_branch="$(git branch --show-current)"
fi

if [[ -z "$base_branch" && "$current_branch" =~ ^prompt-([0-9]{2})-(.+)$ ]]; then
  prompt_number="${BASH_REMATCH[1]}"
  previous_number=$((10#$prompt_number - 1))
  if (( previous_number > 0 )); then
    previous_prefix="$(printf 'prompt-%02d-' "$previous_number")"
    mapfile -t candidate_branches < <(git for-each-ref --format='%(refname:strip=3)' refs/remotes/origin | grep "^${previous_prefix}" | sort)
    if [[ ${#candidate_branches[@]} -eq 1 ]]; then
      base_branch="${candidate_branches[0]}"
    fi
  fi
fi

if [[ -z "$base_branch" ]]; then
  echo "Não foi possível identificar a branch base. Configure SONAR_BASE_BRANCH." >&2
  exit 1
fi

git fetch --no-tags origin "+refs/heads/$base_branch:refs/remotes/origin/$base_branch" >/dev/null 2>&1 || true
base_ref="origin/$base_branch"
git rev-parse --verify "$base_ref^{commit}" >/dev/null 2>&1 || {
  echo "A branch base '$base_branch' não existe no origin." >&2
  exit 1
}
git rev-parse --verify "$head_commit^{commit}" >/dev/null 2>&1 || {
  echo "O commit atual '$head_commit' não existe no checkout." >&2
  exit 1
}

base_commit="$(git rev-parse "$base_ref^{commit}")"
head_commit="$(git rev-parse "$head_commit^{commit}")"
if [[ "$base_commit" == "$head_commit" ]]; then
  echo "A branch atual e a branch base apontam para o mesmo commit; não há impacto para analisar." >&2
  exit 0
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
  local analysis_id=""
  local task_status
  local measures

  git checkout --detach --force "$commit" >/dev/null
  rm -rf target

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
    "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"
  )
  if [[ -n "${SONAR_ORGANIZATION:-}" ]]; then
    sonar_arguments+=("-Dsonar.organization=$SONAR_ORGANIZATION")
  fi

  "${mvn_command[@]}" -B verify \
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

  [[ -n "$analysis_id" && "$analysis_id" != "null" ]] || {
    echo "A análise SonarQube não terminou para $commit" >&2
    return 1
  }

  analysis_metrics="$(curl --fail --silent --show-error \
    --header "Authorization: Bearer $SONAR_TOKEN" \
    --get "$SONAR_HOST_URL/api/measures/component" \
    --data-urlencode "component=$project_key" \
    --data-urlencode "metricKeys=$metrics")"
}

git cat-file -e "$base_commit:pom.xml" || { echo "A branch base não contém pom.xml." >&2; exit 1; }
git cat-file -e "$head_commit:pom.xml" || { echo "A branch atual não contém pom.xml." >&2; exit 1; }

run_analysis "$base_commit" "base-${base_commit:0:12}"
base_metrics="$analysis_metrics"
run_analysis "$head_commit" "head-${head_commit:0:12}"
head_metrics="$analysis_metrics"

metric_value() {
  number_or_na "$(json_value "$1" "$2")"
}

base_bugs="$(metric_value "$base_metrics" bugs)"
head_bugs="$(metric_value "$head_metrics" bugs)"
base_vulnerabilities="$(metric_value "$base_metrics" vulnerabilities)"
head_vulnerabilities="$(metric_value "$head_metrics" vulnerabilities)"
base_hotspots="$(metric_value "$base_metrics" security_hotspots)"
head_hotspots="$(metric_value "$head_metrics" security_hotspots)"
base_smells="$(metric_value "$base_metrics" code_smells)"
head_smells="$(metric_value "$head_metrics" code_smells)"
base_coverage="$(metric_value "$base_metrics" coverage)"
head_coverage="$(metric_value "$head_metrics" coverage)"
base_duplication="$(metric_value "$base_metrics" duplicated_lines_density)"
head_duplication="$(metric_value "$head_metrics" duplicated_lines_density)"
base_complexity="$(metric_value "$base_metrics" complexity)"
head_complexity="$(metric_value "$head_metrics" complexity)"
base_ncloc="$(metric_value "$base_metrics" ncloc)"
head_ncloc="$(metric_value "$head_metrics" ncloc)"

base_date="$(git show -s --format=%cI "$base_commit")"
head_date="$(git show -s --format=%cI "$head_commit")"

{
  echo 'branch,role,commit,date,bugs,vulnerabilities,security_hotspots,code_smells,coverage,duplicated_lines_density,complexity,ncloc'
  printf '%s,base,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' "$base_branch" "$base_commit" "$base_date" "$base_bugs" "$base_vulnerabilities" "$base_hotspots" "$base_smells" "$base_coverage" "$base_duplication" "$base_complexity" "$base_ncloc"
  printf '%s,head,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' "$current_branch" "$head_commit" "$head_date" "$head_bugs" "$head_vulnerabilities" "$head_hotspots" "$head_smells" "$head_coverage" "$head_duplication" "$head_complexity" "$head_ncloc"
} > "$reports_dir/branches.csv"

{
  echo '# Impacto de qualidade entre branches'
  echo
  echo "Projeto SonarQube: \`$project_key\`"
  echo
  echo "Comparação: \`$base_branch\` (${base_commit:0:12}) → \`$current_branch\` (${head_commit:0:12})"
  echo
  echo '| Métrica | Branch base | Branch atual | Delta |'
  echo '|---|---:|---:|---:|'
  printf '| Bugs | %s | %s | %s |\n' "$base_bugs" "$head_bugs" "$(delta "$head_bugs" "$base_bugs")"
  printf '| Vulnerabilidades | %s | %s | %s |\n' "$base_vulnerabilities" "$head_vulnerabilities" "$(delta "$head_vulnerabilities" "$base_vulnerabilities")"
  printf '| Hotspots | %s | %s | %s |\n' "$base_hotspots" "$head_hotspots" "$(delta "$head_hotspots" "$base_hotspots")"
  printf '| Code smells | %s | %s | %s |\n' "$base_smells" "$head_smells" "$(delta "$head_smells" "$base_smells")"
  printf '| Cobertura | %s | %s | %s |\n' "$base_coverage" "$head_coverage" "$(delta "$head_coverage" "$base_coverage")"
  printf '| Duplicação | %s | %s | %s |\n' "$base_duplication" "$head_duplication" "$(delta "$head_duplication" "$base_duplication")"
  printf '| Complexidade | %s | %s | %s |\n' "$base_complexity" "$head_complexity" "$(delta "$head_complexity" "$base_complexity")"
  printf '| Linhas | %s | %s | %s |\n' "$base_ncloc" "$head_ncloc" "$(delta "$head_ncloc" "$base_ncloc")"
  echo
  echo '## Alterações de código entre as branches'
  echo
  git diff --stat "$base_commit" "$head_commit"
  echo
  echo '## Arquivos alterados'
  echo
  git diff --name-status "$base_commit" "$head_commit"
} > "$reports_dir/impacto-branches.md"

git checkout --detach --force "$head_commit" >/dev/null
