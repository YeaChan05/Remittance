#!/usr/bin/env bash

set -euo pipefail

root_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
settings_file="${root_dir}/settings.gradle.kts"

prompt() {
  local label="$1"
  local value=""
  read -r -p "${label}" value
  printf '%s' "$value"
}

domain=$(prompt "Enter domain name: ")
if [[ -z "$domain" ]]; then
  echo "Domain name is required." >&2
  exit 1
fi

if [[ ! "$domain" =~ ^[a-z0-9][a-z0-9_-]*$ ]]; then
  echo "Domain name allows lowercase letters, numbers, '-' and '_' only." >&2
  exit 1
fi

artifact=$(prompt "Enter artifact package (e.g., com.example.remittance): ")
if [[ -z "$artifact" ]]; then
  echo "Artifact package is required." >&2
  exit 1
fi

if [[ ! "$artifact" =~ ^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+$ ]]; then
  echo "Artifact must be a valid package format." >&2
  exit 1
fi

artifact_path=${artifact//./\/}

application_type=$(prompt "Enter application type (api/batch): ")
case "$application_type" in
  api|batch) ;;
  *)
    echo "Application type must be api or batch." >&2
    exit 1
    ;;
esac

repository_input=$(prompt "Enter infrastructure implementation (comma/space separated, now only jpa supported): ")
repository_input="${repository_input//,/ }"
read -r -a repository_types <<<"$repository_input"

if [[ ${#repository_types[@]} -eq 0 ]]; then
  echo "Enter at least one repository type." >&2
  exit 1
fi

repository_types_unique=()
repository_seen=""
for repo_type in "${repository_types[@]}"; do
  repo_type=$(printf '%s' "$repo_type" | tr '[:upper:]' '[:lower:]')
  [[ -z "$repo_type" ]] && continue
  if [[ "$repo_type" != "jpa" ]]; then
    echo "Only 'jpa' repository type is supported: $repo_type" >&2
    exit 1
  fi
  if [[ " $repository_seen " != *" ${repo_type} "* ]]; then
    repository_types_unique+=("$repo_type")
    repository_seen="${repository_seen} ${repo_type}"
  fi
done

if [[ ${#repository_types_unique[@]} -eq 0 ]]; then
  echo "Enter at least one repository type." >&2
  exit 1
fi

domain_dir="${root_dir}/${domain}"
if [[ -e "$domain_dir" ]]; then
  echo "Domain directory already exists: $domain_dir" >&2
  exit 1
fi

create_module_dirs() {
  local module_dir="$1"
  local include_integration="${2:-false}"
  mkdir -p "${module_dir}/src/main/kotlin/${artifact_path}" "${module_dir}/src/main/resources"
  mkdir -p "${module_dir}/src/test/kotlin/${artifact_path}" "${module_dir}/src/test/resources"
  if [[ "$include_integration" == "true" ]]; then
    mkdir -p "${module_dir}/src/integrationTest/kotlin/${artifact_path}" "${module_dir}/src/integrationTest/resources"
  fi
}

create_application_dirs() {
  local module_dir="$1"
  local include_integration="${2:-false}"
  mkdir -p "${module_dir}/src/main/kotlin/${artifact_path}" "${module_dir}/src/main/resources"
  if [[ "$include_integration" == "true" ]]; then
    mkdir -p "${module_dir}/src/integrationTest/kotlin/${artifact_path}" "${module_dir}/src/integrationTest/resources"
  fi
}

write_gradle_properties() {
  local module_dir="$1"
  local module_type="$2"
  local label="$3"
  {
    echo "type=${module_type}"
    if [[ -n "$label" ]]; then
      echo "label=${label}"
    fi
    echo "group=${artifact}.${domain}"
  } > "${module_dir}/gradle.properties"
}

write_build_gradle() {
  local module_dir="$1"
  shift
  {
    echo "dependencies {"
    for line in "$@"; do
      echo "    ${line}"
    done
    echo "}"
  } > "${module_dir}/build.gradle.kts"
}

mkdir -p "$domain_dir"
{
  echo "group=${artifact}.${domain}"
} > "${domain_dir}/gradle.properties"

module_list=(model infrastructure service exception api)
if [[ "$application_type" == "batch" ]]; then
  module_list+=(batch)
fi

for repo_type in "${repository_types_unique[@]}"; do
  module_list+=("repository-${repo_type}")
done

module_list+=("application-${application_type}")

for module in "${module_list[@]}"; do
  mkdir -p "${domain_dir}/${module}"
done

create_module_dirs "${domain_dir}/model"
write_build_gradle "${domain_dir}/model"
write_gradle_properties "${domain_dir}/model" "java-lib" ""

create_module_dirs "${domain_dir}/infrastructure"
write_build_gradle "${domain_dir}/infrastructure" "api(project(\":${domain}:model\"))"
write_gradle_properties "${domain_dir}/infrastructure" "java" ""

create_module_dirs "${domain_dir}/service"
write_build_gradle "${domain_dir}/service" \
  "api(project(\":${domain}:model\"))" \
  "implementation(project(\":${domain}:infrastructure\"))" \
  "implementation(project(\":${domain}:exception\"))"
write_gradle_properties "${domain_dir}/service" "java-boot" ""

create_module_dirs "${domain_dir}/exception"
write_build_gradle "${domain_dir}/exception"
write_gradle_properties "${domain_dir}/exception" "java" ""

create_module_dirs "${domain_dir}/api"
write_build_gradle "${domain_dir}/api" \
  "implementation(project(\":${domain}:exception\"))" \
  "implementation(project(\":${domain}:service\"))"
write_gradle_properties "${domain_dir}/api" "java-boot-mvc" ""

if [[ "$application_type" == "batch" ]]; then
  create_module_dirs "${domain_dir}/batch"
  write_build_gradle "${domain_dir}/batch" \
    "implementation(project(\":${domain}:exception\"))" \
    "implementation(project(\":${domain}:service\"))"
  write_gradle_properties "${domain_dir}/batch" "java-boot-batch" ""
fi

for repo_type in "${repository_types_unique[@]}"; do
  repo_dir="${domain_dir}/repository-${repo_type}"
  include_repo_integration="false"
  if [[ "$repo_type" == "jpa" ]]; then
    include_repo_integration="true"
  fi
  create_module_dirs "$repo_dir" "$include_repo_integration"
  write_build_gradle "$repo_dir" \
    "implementation(project(\":${domain}:infrastructure\"))"
  write_gradle_properties "$repo_dir" "java-boot-repository-${repo_type}" ""
done

application_dir="${domain_dir}/application-${application_type}"
include_app_integration="false"
if [[ "$application_type" == "api" ]]; then
  include_app_integration="true"
fi
create_application_dirs "$application_dir" "$include_app_integration"

application_deps=("implementation(project(\":${domain}:${application_type}\"))")
for repo_type in "${repository_types_unique[@]}"; do
  application_deps+=("implementation(project(\":${domain}:repository-${repo_type}\"))")
done

write_build_gradle "$application_dir" "${application_deps[@]}"

if [[ "$application_type" == "api" ]]; then
  application_module_type="java-boot-mvc-application"
else
  application_module_type="java-boot-batch-application"
fi

write_gradle_properties "$application_dir" "$application_module_type" "docker"

if [[ -f "$settings_file" ]]; then
  include_lines=()
  for module in "${module_list[@]}"; do
    include_line="include(\":${domain}:${module}\")"
    if ! grep -qF "$include_line" "$settings_file"; then
      include_lines+=("$include_line")
    fi
  done

  if [[ ${#include_lines[@]} -gt 0 ]]; then
    include_block=$(printf '%s\n' "${include_lines[@]}")
    tmp_file=$(mktemp)
    awk -v block="$include_block" '
      /^pluginManagement \{/ && !inserted {
        print block
        inserted = 1
      }
      { print }
      END { if (!inserted) print block }
    ' "$settings_file" > "$tmp_file"
    mv "$tmp_file" "$settings_file"
  fi
fi

echo "Modules created: ${domain} (${application_type})"
