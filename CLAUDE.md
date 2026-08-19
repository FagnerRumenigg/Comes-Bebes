# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Compress Tool Output

When executing tools, compress outputs to save tokens:

- *Skip verbose headers* — remove unnecessary framework/tool information
- *Truncate long lists* — show only first 5-10 items with a count of remaining
- *Omit successful confirmations* — skip "success" messages unless critical
- *Abbreviate file paths* — use relative paths instead of full paths
- *Summarize repetitive output* — group similar lines with a count
- *Remove debug output* — strip timestamps, debug info, verbose logging
- *Only show actual results* — errors, key data, status only

Example: Instead of showing 100 lines of log output, show: "✓ Build succeeded (234 files processed, 3 warnings)"

## Compress LLM Output (Caveman Mode)

Keep responses concise and token-efficient:

- *Short sentences* — avoid elaboration, use direct language
- *Single sentences per idea* — break up complex thoughts
- *No fluff* — skip pleasantries, acknowledgments, transitions
- *Show don't tell* — present code/results instead of explaining
- *Lists over prose* — use bullet points instead of paragraphs
- *Direct answers* — respond to the exact question asked
- *Code first* — when writing code, minimize commentary
- *One summary line* — end with a single-line summary when done

Example: ✓ Done. (vs. "I've successfully completed the task as requested...")

## Infrastructure & Deploy Context

- Production is Azure (Container Apps + ACR + Postgres Flexible Server + Blob Storage), since the 2026-08-19 cutover. Local Docker/Postgres (`backend/docker-compose.yml`, the shared `Infra-Geral` stack) is for local testing only — never treat it as production or assume it serves real traffic.
- ngrok and Docker Hub are retired for this project's deploy. Don't reintroduce them, don't propose them as a fix, and don't describe them as current in new code or docs.
- `docs/DEPLOY.md` is the single source of truth for how infra/deploy actually works today. Read it before making infra/CI-CD changes, and update it in the same change whenever deploy flow, CI/CD, or Azure resources change — don't let it drift into another stale doc.
- Deploy to production is fully automatic from a Git tag (`api-v*`/`validator-v*`) — no manual step on any server. Don't invent or restore a manual deploy step without checking `docs/DEPLOY.md` first.

## Workflow: Implementing a Backlog Item (IDEIA-XXX) or Any Deployable Change

- After implementing, start the backend and frontend locally (`backend/docker-compose.yml` and/or `mvn spring-boot:run`, `npm run dev`) and **leave them running** so the user can test in the browser themselves. Don't just hand over a written summary — give them a live environment to click through.
- Wait for the user's explicit "ok" (or equivalent) before doing anything below. Don't deploy on your own judgment call that it "looks done."
- Once approved, do all three, every time:
  1. **Ship it** — merge the branch to `main` (direct merge, no PR — see below), push, and tag per SemVer (`api-v*`/`validator-v*`) for backend changes. This triggers the fully automatic deploy (see `docs/DEPLOY.md`); confirm it actually went live.
  2. **Add a patch note** — insert a row into `application.patch_notes` (see `docs/DEPLOY.md` → "Notas de versão") so the change shows up in the "novidades" modal on the user's next login.
  3. **Mark it done** — update `docs/BACKLOG.md`: flip the item's table row to ✅ Concluída and add/update its detail section (branch name, what actually shipped vs. what was planned), matching the style of existing entries.

## No PR — Direct Merge

This repo has no pull-request step. Each backlog item gets its own branch (`ideia/XXX-nome-curto`); once approved it merges straight into `main`. The merge/integration commit message should be thorough (it substitutes for the review a PR would normally carry).
