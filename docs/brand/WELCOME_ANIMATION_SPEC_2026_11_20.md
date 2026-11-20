# MangoLoads welcome motion — precise one-shot choreography

**Design stance:** The app icon is a stable, recognizable mark. Animation belongs to the **in-app welcome**, not every launcher render, Settings navigation, or keystroke. Motion should feel quiet, tactile and intentional—premium, not animated wallpaper. Current code's endlessly spinning Material key shapes do not preserve the new logo and are retired.

## Surface policy

| Surface | Motion decision | Why |
|---|---|---|
| Launcher, app drawer, recent apps, Android Settings | **Static** adaptive MangoLoads icon | The OS owns mask/entry motion; no custom animation or battery work. |
| Android 12+ platform splash | **Static**, system-managed icon | Fastest cold start; avoid double splash or unsupported animated-glass layering. |
| First-run welcome hero | **One 700–850ms staged animation** (see timeline) | This is the one place where the identity can come to life without interfering with typing. |
| Welcome → Enable/Switch setup | **Existing slide+fade, refined** | Continuity, not a new spectacle. |
| Setup checkbox success | **Optional ~150ms state feedback** | Subtle acknowledgment only on actual completed step; do not replay on IME polling. |
| Setup Essential Settings expand | **Keep existing 300–350ms collapse/chevron** unless profiling shows a reason to tune | Already functional, low scope/risk. |
| Settings header / About left icon / About app row | **Static**; existing row ripple only | Brand recognition and responsiveness matter more than movement. |
| IME keyboard, Emoji, microphone, Clipboard, incognito | **No brand-specific animation** | Keep functional animation and latency contracts independent. |
| README/social/store art | **Static image** | Animated WebP/GIF logo is unnecessary and inconsistent with platform behavior. |

## The welcome timeline (approximately 800ms total)

All visual parts use **one shared frame clock**, with separate interpolation channels and no independent infinite loops. The final frame must be visually the same as the approved full-color icon. Timing is a default design target, adjusted after visual preview and reduced-motion tests.

| Time from entry | Property | Proposed curve | Optical intention |
|---|---|---|---|
| 0ms | Show normal welcome copy; reserve the logo's final 320dp-responsive layout bounds | No layout motion | Text/buttons do not jump as artwork appears. |
| 0–160ms | Blue keyboard+keycaps `alpha 0→1`, `scale 0.965→1.0`, `translationY +6dp→0` | Smooth decelerate | Keyboard settles gently, no elastic overshoot and no bounce per key. |
| 100–310ms | White keys' *ink only* `alpha 0.85→1` (if supported by approved separated layer) | Ease-out | Letters become crisp early; never progressively garble/reorder `MANGO`/`LOADS`. If the layer split is not excellent, skip this channel. |
| 160–510ms | Glass badge + mango as a **joined unit** `alpha 0→1`, `translationY −10dp→0`, `scale 0.975→1` | Critically damped spring or cubic ease-out | Badge appears to seat naturally over the three center top keys; no fruit detaching from badge. |
| 300–620ms | Shield rim highlight `alpha 0→~0.42→0` within the exact shield clip | Soft single pulse, **one time** | Hint of liquid glass/refraction; never flash over unshielded A/N/G or blur outside mask. |
| 400–750ms | Background radial halo `alpha 0.12→0.20→0.12` (theme controlled) | Smooth tween | Depth without pulsing the icon itself. Could omit if it reduces contrast. |
| 750–850ms | Snap all animated properties to canonical steady values, cancel work | Settle | Entire artwork sits motionless until user leaves or re-enters wizard. |

### Explicitly rejected motion patterns

- Infinite 2300ms breathing across the whole screen (current `SplashScreenView`) and random rotating/morphing shapes in `AnimatedBoardIcon` (current 1150ms cycle): the mark must not shift identity after 1 second.
- A bright laser-like diagonal sweep, continuous glass wobble, falling mango, bouncing keys, droplets, moving text, mouse-tracked parallax, or rotating the brand shield. They add visual noise and risk destroying exact key text.
- Per-frame bitmap regeneration, RenderEffect blur, screen-copy/capture, live host-app refraction, complex shader compilation, large animated WebP/Lottie dependency, 60fps GPU work while IME is active, or a permanent coroutine after leaving welcome.
- A loading spinner or intro delay before the **Let's get you started** action becomes tappable. Touches must respond immediately even if intro has not ended.

## Implementation mechanics for Compose

1. Keep a **single brand component** such as `MangoLoadsBrandMark` that renders properly sized, cached, immutable PNG/vector layers; it is shared by static usages and the welcome host so the final frame is consistent. Make accessibility description configurable; use null when adjacent heading already says brrBoard.
2. `AnimatedMangoLoadsMark` is a thin wrapper applying `graphicsLayer` alpha/translation/scale to brand layers. `AnimatedVisibility` is not needed for every component; it can trigger repeated measurement. Put each layer in a fixed-size `Box` and transform only draw-time properties.
3. `remember` resource painters/immutable geometry. Use one `LaunchedEffect(wizardEntryId, animationsEnabled)` to drive bounded `Animatable` values or an equivalent finite Compose transition. Compose cancels the effect when leaving composition; never launch global/background scope animators.
4. On `isAnimating=false`, leaving Splash, `ON_STOP`, or reduced-motion mode: cancel, set final state, do not start a new loop. `rememberSaveable` must not restart intro simply because a preference/IME enabled state polls. On an intentional re-entry from Settings, a fresh wizard-entry identity may replay once.
5. Use Android's actual animator-duration scale / supported Compose motion scale. When animation is disabled, show the complete full mark on the **first rendered frame**, including mango and glass, and preserve navigation/controls. Do not assume `0` means the app is broken.
6. `Canvas` draw rects, gradients and clipping only when static resource layers cannot faithfully represent the approved icon. The glass's optical blur is precomputed and masked **inside its bounds**. Animated highlight is a translucent clipped overlay; it is not dynamic refraction of live content.
7. Keep `contentScale = Fit` and uniform scale; no stretch. Sample icon at actual device density. Art is square, with layer positions in normalized relative coordinates so the same full mark works at 128, 256 and 320dp. Align artwork center; account for system bars and window insets.

## Screen transition contract

- Current `WelcomeWizard.kt` has `AnimatedContent` with 30dp horizontal slide and 300ms fade. It is already direction-aware. Proposed modest refinement: 20dp translation, ~240–300ms tween/spring, avoiding overshoot. Do not change the route state machine or button labels.
- Starting `Setup` at any point interrupts and disposes the logo's entry motion. The background halo finishes with the Splash screen; it must not linger during the next screen.
- Returning from Setup to Splash should reveal a coherent full mark immediately or replay its one-shot sequence once (choose via visual test); **never** reset to half-morphed/random keycaps.
- Enable-IME and Switch-IME settings intents may pause/recreate the activity. Wizard remains operable, preserves enabled/current state, avoids duplicate navigation and out-of-order animations.
- Setup checkmarks: if tested, animate only true state transitions (`false→true`) with one 130–170ms opacity/scale confirmation and leave completion static. For external IME picker polling or activity resume, compare old/new state so a transient false does not generate repeated twitching. No haptics added automatically.
- Current `AnimatedVisibility` Essential Settings has a 350ms expand/fade and 300ms chevron rotation. Keep unless actual hardware traces show jank; this design change is not an excuse to rewrite unrelated onboarding settings.

## Performance & accessibility acceptance

- No extra CPU/GPU brand work while keyboard is visible. Static Settings rows do **zero animation work** when idle. No new startup network or image decoder on main thread from arbitrary files.
- Use pre-sized assets: small row should not decode a giant 1024px hero. Cache the large welcome layers for current screen, release when removed. Record heap delta and GPU overdraw on Oppo.
- Benchmark `SettingsActivity` cold/warm start and welcome first interactive frame before/after. Do not impose delay. Inspect `adb shell dumpsys gfxinfo`, Macrobenchmark if existing environment supports it, and 60/90Hz frame timings using real device traces.
- Smooth exit, no skipped frames visible in 240ms screen transition, rapid double tap or back gesture. With animator scale `0`, no motion; at `.5×` and `2×`, content remains synchronized. Battery saver, rotation, dark/light, TalkBack, high font scale and split-screen must not cause clipped/doubled imagery.
- All 10 key letters and `brr` remain stable and readable on the final frame. The glass shield is the only localized refractive region; no fuzzy text outside it at any point, including motion.

## Previews required before publication

1. Five captured keyframes: 0ms, ~160ms, ~350ms, ~600ms and finished ~800ms; compare geometry to approved static composite.
2. A short *actual* Oppo screen recording of welcome entry and pressing Let's get you started mid-animation, plus Settings → Setup Wizard re-entry.
3. A 0-animation-scale recording. The full logo must be present immediately.
4. Screenshots of final stable logo at 320dp on light and dark screens, at 200% font scale, and in landscape.

**If the animation worsens perceived speed or fidelity, fall back to a 180–240ms simple alpha+scale reveal or entirely static art.** It is more important that the brand feels correct and app remains quick than that every layer moves.
