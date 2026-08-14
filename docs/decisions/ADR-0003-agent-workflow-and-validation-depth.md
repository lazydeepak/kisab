# ADR-0003: Agent Workflow and Validation Depth

## Status
Accepted

## Context
Kisab uses agents to move quickly, but broad validation campaigns can consume attention and make an internal candidate feel like a public release. `v0.2.0` is not automatically a public-release readiness event. Validation must match the intent of the work and the maintainer's acceptance state.

The maintainer remains the product authority for feature behavior and UI/UX acceptance. Agents may prepare, implement, review, and validate work, but they must not turn every candidate into a full release campaign without explicit approval.

## Decision
Use this workflow for future feature, UI/UX, release, and validation work:

1. Maintainer states what they want.
2. Codex prepares bounded agent work: scope, non-goals, acceptance criteria, files to respect, and what not to touch.
3. Agent implements or upgrades within that scope.
4. Codex reviews the agent output for fit, risk, scope creep, and unnecessary validation.
5. Maintainer accepts or rejects function and UI/UX.
6. Only after maintainer acceptance, Codex prepares a targeted validation prompt.
7. Agent validates at the approved depth.

Implementation comes before validation. Maintainer acceptance comes before any broad validation campaign.

## Validation depth
Validation depth must be explicit:

- **Targeted confidence check:** narrow checks for the changed feature or document. This is appropriate during normal Android feature/UI/UX work.
- **Internal candidate check:** broader automated confidence checks for an internal candidate, still not a public-release gate.
- **Public-release gate:** full release validation, device/manual matrix, upgrade campaign, signing/release workflow checks, and repeated release-candidate evidence. Run this only when the maintainer explicitly asks to pursue public release readiness.

For pre-`v0.2.0` work, default to targeted confidence checks unless the maintainer approves a broader validation prompt.

## Agent prompt rule
Before asking another agent to run broad validation, Codex must ask the maintainer for approval and state:

- what will be validated;
- why that depth is needed;
- how long or heavy the validation is expected to be;
- what will remain unvalidated if the maintainer chooses a lighter check.

If the maintainer has not accepted the function or UI/UX yet, do not run a validation campaign to force acceptance. Record the open concern instead.

## Consequences
- Agents stay useful without taking over product judgment.
- Internal candidates can remain lightweight.
- Public-release validation remains available, but only when intentionally chosen.
- Future docs should distinguish "not accepted yet", "accepted for internal candidate", and "public-release ready".
