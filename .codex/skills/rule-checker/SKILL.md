---
name: rule-checker
description: Check whether implemented code follows the copied `references/rule/**` rule documents for module structure, dependency direction, naming, and Spring Boot/JPA/RabbitMQ/Liquibase conventions. Use when reviewing a diff, module, package, `build.gradle.kts`, or implementation for compliance with the copied rule references.
---

# Rule Checker

Check code against the copied rule references under `references/rule/`.
Use the references as the source of truth for compliance checks.

## Inputs

- scope
- changed files or diff
- affected modules
- `build.gradle.kts`
- `gradle.properties`

## Workflow

1. Fix the scope first.
2. Pick only the needed references.
3. Inspect code, Gradle files, package names, annotations, and tests.
4. Report only grounded rule violations.
5. If rules and code disagree, report the mismatch. Do not invent a reconciliation.

## Reference Routing

- module shape / layer intent: `references/rule/module.md`
- dependency rules: `references/rule/module/dependencies.md`
- naming rules: `references/rule/code_convention.md`
- model rules: `references/rule/module/model.md`
- infrastructure rules: `references/rule/module/infrastructure.md`
- service rules: `references/rule/module/service.md`
- exception rules: `references/rule/module/exception.md`
- api rules: `references/rule/module/api.md`
- api-internal rules: `references/rule/module/api-internal.md`
- repository-jpa rules: `references/rule/module/repository-jpa.md`
- mq-rabbitmq rules: `references/rule/module/mq-rabbitmq.md`
- application rules: `references/rule/module/application.md`
- architecture diagram: `references/rule/architecture_guide.puml`

## Checks

- module role mismatch
- forbidden dependency direction
- forbidden cross-domain reference
- wrong adapter placement
- wrong naming suffix / prefix
- missing stack-specific pattern
- Gradle dependency exposure mismatch
- package / module mismatch

## Evidence

- file path
- line reference
- matched rule document
- violating code or dependency

## Output Format

## Checked Scope

## References Used

## Findings

- findings first
- file path
- broken rule
- evidence

## Open Questions

- docs ambiguous
- evidence missing

## Done

- checked references listed
- violations grounded in copied references
- no issue case reported as `문제 없음`
