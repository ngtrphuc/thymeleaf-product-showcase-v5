---
name: smartphone-shop-technical-audit
description: Deep technical audit and review planning for the `smartphone-shop` Spring Boot + Thymeleaf repository. Use when auditing this project for code bugs, logic regressions, data integrity issues, security/privacy risks, performance bottlenecks, architecture drift, duplicate logic, or when proposing repo-aligned remediation plans and new features. Do not use for implementing fixes directly, routine maintenance, or release verification; use the existing maintainer, bugfix, or release skills for those.
---

# Smartphone Shop Technical Audit

## Overview

Audit `smartphone-shop` with a reviewer mindset. Prioritize real findings over summaries, cite exact files and lines, and convert each confirmed issue into a concrete fix plan or feature proposal that fits the existing controller/service/repository/template split.

## Boundaries

- Use this skill for review, audit, duplication analysis, refactor planning, and roadmap design.
- Do not use this skill as the default implementation skill for feature work.
- Do not use this skill for "ship it" or final release checks.
- If the user wants code changes after the audit, hand off mentally to:
  `smartphone-shop-maintainer` for general implementation,
  `smartphone-shop-bugfix-triage` for concrete defects,
  `smartphone-shop-release-checker` for pre-release verification.

## Workflow

1. Read [references/review-playbook.md](references/review-playbook.md) first for architecture, high-risk flows, and review commands.
2. For checkout, cart, wishlist, chat, auth, or admin issues, inspect the service before the controller so validation and transaction boundaries stay clear.
3. Prioritize findings in this order:
- data integrity and stock correctness
- auth, privacy, and payment-data exposure
- state changes during GET or `readOnly` flows
- performance bottlenecks that scale poorly
- UI/template rendering defects
4. Report findings before summaries. For each finding, explain user impact, likely root cause, and the smallest safe fix.
5. When asked for new features, keep proposals compatible with Spring MVC + Thymeleaf + JPA + SSE rather than suggesting a stack rewrite.

## Focus Areas

- Checkout and orders:
  `controller/user/CartController`, `service/CartService`, `service/OrderService`, `model/Order`, `repository/OrderRepository`
- Chat and SSE:
  `service/ChatService`, `controller/admin/ChatAdminController`, `controller/user/ChatUserController`, `templates/admin/chat.html`, `templates/customer/fragments/chat-widget.html`
- Account and payment data:
  `service/AuthService`, `controller/user/ProfileController`, `service/PaymentMethodService`, `model/PaymentMethod`, profile and payment templates
- Catalog and admin maintenance:
  `controller/user/MainController`, `controller/admin/AdminController`, `config/DataInitializer`, `repository/ProductRepository`

## Validation

- Run the whole test suite with `./mvnw.cmd test`.
- Use `rg` to confirm every finding before reporting it.
- If you touch behavior, add or update focused tests where the repo already has coverage (`service/*Test.java`, `controller/*Test.java`).

## References

- Read [references/review-playbook.md](references/review-playbook.md) for architecture, known hotspots, and audit checklist.
- Read [references/skill-boundaries.md](references/skill-boundaries.md) when checking overlap with other `smartphone-shop-*` skills.
- Read [references/fix-plan.md](references/fix-plan.md) when the user wants concrete remediation steps.
- Read [references/feature-roadmap.md](references/feature-roadmap.md) when the user asks for new feature ideas that fit the current project.
