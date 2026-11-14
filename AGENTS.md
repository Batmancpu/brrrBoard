# brrBoard agent instructions

1. At the start of a project task, read [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) and [docs/PROJECT_CONVERSATION.md](docs/PROJECT_CONVERSATION.md). Check the current branch, HEAD, and working tree. Verify relevant code, release, or CI claims before continuing. The context page is the current checkpoint. The conversation record preserves original project requests in order.
2. Update both documents within each project task. In the conversation record, quote project-relevant user requests and corrections verbatim, preserving language and order. Capture the agent's project-relevant decisions, promised deliverables, material instructions, and corrections. Append when the user changes goals, constraints, or priorities, even when no code changes. Mark superseded decisions and keep the original entries.
3. Keep the current goal, acceptance criteria, confirmed state, blockers, and exact next action at the top of PROJECT_CONTEXT.md. Log small changes, attempts, failures, and reversals with paths or symbols, why they changed, exact verification commands and results, relevant commits or CI runs, and any unfinished working-tree changes. Label hardware results as user-reported when applicable. Link [experiment details](docs/EXPERIMENT_LOG.md) and [known issues](docs/KNOWN_ISSUES.md).
4. Separate explicit owner decisions, evidence from the repo, and agent proposals. Carry forward the latest owner decision. Where the full goal is unknown, record the gap. Keep a short resume section current so a new agent can continue without reading every old log immediately.
5. This repository is public. Keep signing keys, passwords, tokens, private messages, and keyboard or clipboard contents out of these files. Use a clear omission note when needed and preserve the project decision or outcome.

## Current product requirements

Follow [the phased brrBoard plan](docs/BRR_BOARD_ROADMAP.md). Keep the keyboard fully offline and preserve the existing Obtainium update identity. Research iOS 27 actual layout and behavior and recover prior button placement from this repository. Use the existing Frosted Glass renderer, with rounded edges and tuned defaults. Implement the local Hinglish dictionary and inline offline dictation. Publish one signed APK per completed phase and update the handoff with what was verified.

Use the exact public label `brrBoard` and the maintainer’s MangoLoads link. Keep historical and license attribution. Prioritize inline on-device dictation and clean onboarding labels.
