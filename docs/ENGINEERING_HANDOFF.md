# brrBoard engineering handoff

## Start here

**Current task is research and planning only.** The owner authorizes documentation-only commits to main. Earlier implementation authorization in the historical records applies to those sessions. A later engineer should verify the current request, main tip, installed version and this checkpoint before editing the app.

| Checkpoint | State | Resume |
| --- | --- | --- |
| R0 — Repository, scope and evidence baseline | Recorded below | R1: read runtime and test paths at pinned source SHA |
| R1 — Source architecture and test triage | Published in this checkpoint | [SOURCE_ARCHITECTURE_AUDIT.md](SOURCE_ARCHITECTURE_AUDIT.md) |
| R2 — Reno10, iOS references and visual system | Published in this checkpoint | [RENO10_VISUAL_SPEC.md](RENO10_VISUAL_SPEC.md) |
| R3 — Dictionaries, Smart Emoji and glide | Published in this checkpoint | [LOCAL_TYPING_ENGINE.md](LOCAL_TYPING_ENGINE.md) |
| R4 — Clipboard, inline autofill, Dictate and lifecycle | Published in this checkpoint | [IME_PROVIDER_INTEGRATIONS.md](IME_PROVIDER_INTEGRATIONS.md) |
| R5 — Ordered implementation and acceptance plan | Published in this checkpoint | [MASTER_IMPLEMENTATION_PLAN.md](MASTER_IMPLEMENTATION_PLAN.md) |
| R6 — Handoff verification | Pending | Confirm docs-only remote history and readiness |

## R0 — Repository baseline on 16 November 2026

- GitHub repository [Batmancpu/brrrBoard](https://github.com/Batmancpu/brrrBoard), ID 1358166218, public, default branch main. GitHub connection reports read and push access. Git and API both resolved source tip [3ee9da9](https://github.com/Batmancpu/brrrBoard/commit/3ee9da9e9b2a0b4a8c42b1e27d93f4686ff65b21), committed at 14:01 IST. The previous source change was [8463fc8](https://github.com/Batmancpu/brrrBoard/commit/8463fc80c1f8695c0f0b11075db25d5e0d3434db), with the intervening 9408a43 documentation checkpoint. No running or queued workflow was visible at discovery. Recent commits show an earlier implementation burst. A live editor or unpushed work cannot be observed. Recheck main and workflow activity before every update.
- Latest signed tag [v1.0.57](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.57), published 14:05 IST by [Obtainium run 43831906150](https://github.com/Batmancpu/brrrBoard/actions/runs/43831906150). Release asset 37,949,189 bytes, SHA-256 a57da3ad314f4f6e21dd64cf18996c5e215134d631d93f4a3a396e75b32bdbfd (GitHub asset metadata). No owner-device acceptance is known for this version.
- [Debug run 43831906039, job 133178974688](https://github.com/Batmancpu/brrrBoard/actions/runs/43831906039/job/133178974688) compiled, verified the debug signer, and uploaded an artifact. Debug APK SHA-256 ee16d7015324dfc54ec5eb0e0968bdbd6d7daab0ee815ae66901148ed90bc40d. The test gate failed 13/180 (5 Parser currency, 7 InputLogic URL/autospace, 1 StringUtils Emoji); the subsequent debug publication step was skipped. Read the actual assertions in R1. The existing release and debug pipelines make distinct promises.
- Current branch inventory: main, codex/incognito-voice-guard-20261115, codex/reno10-ime-boundary-20261115, and jules-board-10408838174702712066. All six listed issues were closed PRs. No branch or tag changed for discovery. Checkpoints use a fast-forward Markdown-only commit and GitHub's commit skip marker because debug push CI includes a publish step and has no path filter. Obtainium's main push filter excludes these documentation files.
- Product target: bring the existing FrostKeys-derived Android IME toward a cohesive iPhone-like typing experience on the owner’s gesture-navigation OPPO Reno10 5G. Keep local typing fast and stable and keep the established frosted-glass identity. The separate experimental Liquid Glass keyboard has its own scope. OPPO India lists CPH2531 at 1080 × 2412 physical pixels with up to 120 Hz screen refresh. The installed OS build, logical density, font scaling, actual refresh and live IME insets require owner-device capture. [OPPO specifications](https://www.oppo.com/in/smartphones/series-reno/reno10/specs/)
- Evidence labels used in this handoff: source inspected at the pinned SHA, workflow/release verified, owner-reported behavior, inherited media description, official reference, and proposal. A reported symptom and a plausible code path are separate claims. No new APK or device run was authorized.
- The supplied mission text ends during F6. The coverage ledger will include the complete visible A–F6 sections and known continuation topics from the repository and earlier owner messages. Unseen wording requires later reconciliation if supplied. The inherited C1–C11 media descriptions are the visual brief. Only sanitized observations belong in this public repository.

## Existing records

- [AGENTS.md](../AGENTS.md) and [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md#resume-here) are agent entry points.
- [Conversation record](PROJECT_CONVERSATION.md) preserves owner decisions and requests in chronological order.
- [Post-v1.0.19 plan](POST_V1_0_19_RENO10_PLAN.md) contains earlier implementation sequencing and many per-batch handoffs. The status table above tracks the current research.
- [Behavior audit](IOS27_BEHAVIOR_AUDIT.md), [iOS/Reno10 plan](IOS27_RENO10_PLAN.md), and [historical roadmap](BRR_BOARD_ROADMAP.md) contain earlier product decisions.
- [Known issues](KNOWN_ISSUES.md) and [experiment log](EXPERIMENT_LOG.md) retain failures, reversals and acceptance gaps.
- [README](../README.md), [layout documentation](../layouts.md), [dictionary provenance](../assets/dictionaries/README.md), and [Obtainium delivery](OBTAINIUM.md) describe distribution and extension points.
- No root context.md or separate research master plan was present in the tracked tree at R0. The linked history remains the record of previous attempts.
