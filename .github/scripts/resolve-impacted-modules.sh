#!/usr/bin/env bash

set -euo pipefail

base_sha=""
head_sha="HEAD"
declare -a changed_files=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      base_sha="${2:-}"
      shift 2
      ;;
    --head)
      head_sha="${2:-HEAD}"
      shift 2
      ;;
    --changed-file)
      changed_files+=("${2:-}")
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

declare -a projects=()
declare -A project_dirs=()
declare -A touched_projects=()
declare -A touch_reasons=()
declare -A full_run_reasons=()
declare -A project_dependencies=()
declare -A dependents_by_project=()
declare -A impacted_projects=()

write_output() {
  local name="$1"
  local value="$2"

  if [[ -z "${GITHUB_OUTPUT:-}" ]]; then
    return
  fi

  local delimiter="EOF_$(date +%s%N)"
  {
    echo "${name}<<${delimiter}"
    printf '%s\n' "$value"
    echo "${delimiter}"
  } >> "$GITHUB_OUTPUT"
}

normalize_file() {
  local file="$1"
  file="${file//\\//}"
  file="${file#./}"
  printf '%s' "$file"
}

load_changed_files() {
  if [[ ${#changed_files[@]} -gt 0 ]]; then
    return
  fi

  if [[ -n "${CHANGED_FILES:-}" ]]; then
    while IFS= read -r line; do
      [[ -n "$line" ]] && changed_files+=("$(normalize_file "$line")")
    done <<< "$CHANGED_FILES"
    return
  fi

  if [[ -z "$base_sha" ]]; then
    return
  fi

  while IFS= read -r line; do
    [[ -n "$line" ]] && changed_files+=("$(normalize_file "$line")")
  done < <(git diff --name-only "${base_sha}..${head_sha}")
}

sort_unique_lines() {
  if [[ $# -eq 0 ]]; then
    return
  fi
  printf '%s\n' "$@" | awk 'NF { if (!seen[$0]++) print $0 }' | sort
}

project_dir() {
  local project="$1"
  printf '%s' "${project#:}" | tr ':' '/'
}

load_projects() {
  while IFS= read -r project; do
    [[ -n "$project" ]] || continue
    projects+=("$project")
    project_dirs["$(project_dir "$project")"]="$project"
    dependents_by_project["$project"]=""
  done < <(sed -n 's/^include("\(:[^"]*\)")/\1/p' settings.gradle.kts | tr -d '\r' | sort)
}

load_dependencies() {
  local project dir build_file deps dep current
  for project in "${projects[@]}"; do
    dir="$(project_dir "$project")"
    deps=()
    for build_file in "$dir/build.gradle.kts" "$dir/build.gradle"; do
      [[ -f "$build_file" ]] || continue
      while IFS= read -r dep; do
        [[ -n "$dep" && "$dep" != "$project" ]] && deps+=("$dep")
      done < <(sed -n 's/.*project("\(:[^"]*\)").*/\1/p' "$build_file" | tr -d '\r')
    done
    current="$(sort_unique_lines "${deps[@]:-}")"
    project_dependencies["$project"]="$current"
    while IFS= read -r dep; do
      [[ -n "$dep" ]] || continue
      dependents_by_project["$dep"]+="${project}"$'\n'
    done <<< "$current"
  done
}

mark_domain_group() {
  local prefix="$1"
  local project
  for project in "${projects[@]}"; do
    [[ "$project" == "${prefix}"* ]] && touched_projects["$project"]=1
  done
}

resolve_touched_projects() {
  local file matched_project domain_root normalized key
  for file in "${changed_files[@]}"; do
    normalized="$(normalize_file "$file")"
    [[ -n "$normalized" ]] || continue

    case "$normalized" in
      build-logic/*|gradle/*|.github/workflows/*|settings.gradle.kts|build.gradle.kts|gradle.properties|.sdkmanrc)
        full_run_reasons["Global build configuration changed: $normalized"]=1
        continue
        ;;
      common/*)
        full_run_reasons["Common module changed: $normalized"]=1
        continue
        ;;
      aggregate/*)
        touched_projects[":aggregate"]=1
        touch_reasons["$normalized -> :aggregate"]=1
        continue
        ;;
      docs/*|.codex/*)
        touch_reasons["$normalized -> non-functional change"]=1
        continue
        ;;
    esac

    matched_project=""
    while IFS= read -r key; do
      [[ -n "$key" ]] || continue
      if [[ "$normalized" == "$key" || "$normalized" == "$key/"* ]]; then
        matched_project="${project_dirs[$key]}"
        break
      fi
    done < <(printf '%s\n' "${!project_dirs[@]}" | awk '{ print length, $0 }' | sort -rn | cut -d" " -f2-)

    if [[ -n "$matched_project" ]]; then
      touched_projects["$matched_project"]=1
      touch_reasons["$normalized -> $matched_project"]=1
      continue
    fi

    domain_root="${normalized%%/*}"
    case "$domain_root" in
      account)
        mark_domain_group ":account:"
        touch_reasons["$normalized -> :account:*"]=1
        ;;
      transfer)
        mark_domain_group ":transfer:"
        touch_reasons["$normalized -> :transfer:*"]=1
        ;;
      member)
        mark_domain_group ":member:"
        touch_reasons["$normalized -> :member:*"]=1
        ;;
      auth)
        mark_domain_group ":auth:"
        touch_reasons["$normalized -> :auth:*"]=1
        ;;
      *)
        full_run_reasons["Unmapped path changed: $normalized"]=1
        ;;
    esac
  done
}

expand_impacted_projects() {
  local -a queue=()
  local current dependent
  mapfile -t queue < <(sort_unique_lines "${!touched_projects[@]}")

  while [[ ${#queue[@]} -gt 0 ]]; do
    current="${queue[0]}"
    queue=("${queue[@]:1}")

    [[ -n "$current" ]] || continue
    [[ -n "${impacted_projects[$current]:-}" ]] && continue
    impacted_projects["$current"]=1

    while IFS= read -r dependent; do
      [[ -n "$dependent" ]] || continue
      [[ -n "${impacted_projects[$dependent]:-}" ]] && continue
      queue+=("$dependent")
    done <<< "${dependents_by_project[$current]:-}"
  done
}

join_tasks() {
  local suffix="$1"
  shift
  local project
  local tasks=()
  for project in "$@"; do
    [[ -n "$project" ]] && tasks+=("${project}:${suffix}")
  done
  printf '%s' "${tasks[*]:-}"
}

print_summary() {
  echo "Changed files:"
  if [[ ${#changed_files[@]} -eq 0 ]]; then
    echo "- (none)"
  else
    sort_unique_lines "${changed_files[@]}" | sed 's/^/- /'
  fi

  echo
  echo "Touched modules:"
  if [[ ${#touched_projects[@]} -eq 0 ]]; then
    echo "- (none)"
  else
    sort_unique_lines "${!touched_projects[@]}" | sed 's/^/- /'
  fi

  if [[ ${#touch_reasons[@]} -gt 0 ]]; then
    echo
    echo "Touch reasons:"
    sort_unique_lines "${!touch_reasons[@]}" | sed 's/^/- /'
  fi

  echo
  echo "Impacted modules:"
  if [[ "$skip_run" == "true" ]]; then
    echo "- (skip)"
  elif [[ "$full_run" == "true" ]]; then
    echo "- (full run)"
  else
    sort_unique_lines "${!impacted_projects[@]}" | sed 's/^/- /'
  fi

  echo
  echo "Full run reasons:"
  if [[ ${#full_run_reasons[@]} -eq 0 ]]; then
    echo "- (none)"
  else
    sort_unique_lines "${!full_run_reasons[@]}" | sed 's/^/- /'
  fi

  echo
  echo "Final Gradle commands:"
  if [[ "$skip_run" == "true" ]]; then
    echo "- (skip)"
  elif [[ "$full_run" == "true" ]]; then
    echo "- ./gradlew test integrationTest --parallel"
  else
    echo "- ./gradlew $verification_tasks --parallel"
  fi
}

load_changed_files
if [[ ${#changed_files[@]} -gt 0 ]]; then
  mapfile -t changed_files < <(sort_unique_lines "${changed_files[@]}")
fi
load_projects
load_dependencies
resolve_touched_projects

skip_run="false"
full_run="false"
assemble_tasks=""
test_tasks=""
integration_test_tasks=""
verification_tasks=""

if [[ ${#changed_files[@]} -eq 0 ]]; then
  skip_run="true"
elif [[ ${#full_run_reasons[@]} -gt 0 ]]; then
  full_run="true"
elif [[ ${#touched_projects[@]} -eq 0 ]]; then
  skip_run="true"
else
  expand_impacted_projects
  mapfile -t impacted_list < <(sort_unique_lines "${!impacted_projects[@]}")
  mapfile -t integration_impacted_list < <(sort_unique_lines "${!impacted_projects[@]}" ":aggregate")
  assemble_tasks="$(join_tasks "assemble" "${impacted_list[@]}")"
  test_tasks="$(join_tasks "test" "${impacted_list[@]}")"
  integration_test_tasks="$(join_tasks "integrationTest" "${integration_impacted_list[@]}")"
  verification_tasks="$(printf '%s %s' "$test_tasks" "$integration_test_tasks" | xargs)"
fi

summary="$(print_summary)"
printf '%s\n' "$summary"

write_output "full_run" "$full_run"
write_output "skip_run" "$skip_run"
write_output "assemble_tasks" "$assemble_tasks"
write_output "test_tasks" "$test_tasks"
write_output "integration_test_tasks" "$integration_test_tasks"
write_output "verification_tasks" "$verification_tasks"
write_output "summary" "$summary"
