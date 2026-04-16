# Skill Boundaries

## Goal

Keep the local audit skill narrow so it does not duplicate the existing global `smartphone-shop-*` skills.

## Current Result

- Exact duplicate names: none found.
- Closest overlap before refinement:
  `smartphone-shop-maintainer`
  `smartphone-shop-bugfix-triage`
  `smartphone-shop-release-checker`
- Resolution:
  narrow this local skill to audit, duplication analysis, and planning only.

## Routing Guide

### Use `smartphone-shop-technical-audit` for

- full code review
- logic/security/performance audit
- duplicate code or duplicate skill-scope analysis
- refactor planning before implementation
- feature roadmap proposals tied to current architecture

### Use `smartphone-shop-maintainer` for

- normal implementation work
- refactors that are already approved
- coordinated backend + template + static asset edits

### Use `smartphone-shop-bugfix-triage` for

- a concrete reported defect
- reproduction and smallest-safe patch
- regression-test-oriented fixes

### Use `smartphone-shop-release-checker` for

- merge readiness
- release sanity checks
- build/test/runtime verification before shipping

## Duplicate Handling Rule

If two skills seem applicable:

1. Prefer the more execution-oriented skill when the user already wants changes.
2. Prefer this audit skill only when the user first wants diagnosis, findings, deduplication, or a remediation plan.
3. Avoid storing the same project architecture notes in multiple local skills unless they are required by different workflows.
