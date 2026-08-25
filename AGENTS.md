# Kisab Agent Contract

Kisab is a standalone offline-first Android farm ledger app (`com.susankhya.kisab`) built on the shared Susankhya foundation for technical concerns only. Farm semantics live in this repository.

## Ground truth and routing

- Code, tests, schema, and Git history are implementation ground truth. Documentation may lag; verify against it before substantive work.
- Check Git state first: current branch, dirty files, recent log. Do not assume state from summaries.
- Current work is routed through `docs/CURRENT.md`. Start there. If it conflicts with Git reality, fix the router or report the discrepancy — do not silently pick one.
- Old milestone and validation records under `docs/milestones/` and `docs/validation/` describe completed or scoped work, not current requirements. Treat them as evidence and context only.
- Archives and project history are investigation-only. Do not mine them for active tasks.

## Workflow and validation

- Detailed agent workflow, review order, and validation-depth policy live in `docs/decisions/ADR-0003-agent-workflow-and-validation-depth.md`. Follow it: implementation → maintainer acceptance → approved validation depth. Do not launch broad validation campaigns without explicit approval.
- Normal validation gate: `./gradlew :app:verifyLocal`.
- UI/UX changes require real-device validation where applicable; record evidence under `docs/validation/`.

## Loading discipline

Load architecture, design, decision, and localization documents only when relevant to the task at hand. Do not sweep `docs/` wholesale into context.

## Multi-agent rules

- One writer per worktree. Read-only reviewers may operate anywhere.
- Durable project-state changes (milestone dispositions, router updates, backlog moves) must be committed and reconciled before handoff to another agent or session.

## Pointers

- Current work: `docs/CURRENT.md`
- Deferred/future work: `docs/BACKLOG.md`
- Product charter: `docs/charter/Kisab-Product-Charter.md`
- Build, signing, and release operations: `README.md` and `docs/release/`
