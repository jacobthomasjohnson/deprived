# Changelog

## 1.3.1

Version 1.3.1 completes the full Deprived rename: mod ID, Java namespace, commands, configuration, assets, registries and networking now use `deprived`.

## 1.3 (Control Room)

- Press `F6`, or select Config from Mod Menu, to open the live Deprived Control Room.
- Visual toggles and sliders apply immediately while the world remains visible behind the menu.
- The System page controls pacing, audio distance, progression thresholds and extreme-stage damage; Apply sends them through an operator-checked server update.
- The Developer page sets test stages and immediately triggers individual audio, visual and hallucination events.
- A live analog-fault preview makes visual tuning possible without waiting for a random event.

## 1.2

- A staged analog-signal layer: exposure breathing, rolling interference, chromatic edge bleed, tracking tears, phosphor trails, film burns and short channel-drift episodes.
- Analog faults remain sparse at first and grow less stable with severe deprivation instead of becoming a permanent filter.
- Moving edge noise replaces fixed vignette lines.
- The developer HUD is restyled as a muted cyan/magenta signal monitor and reports the active analog fault.
- Every analog family can be disabled independently. `analogEffectIntensity` scales the complete treatment from 0–100.
- v1.2.1 adds a rebindable developer shortcut: press `F8` to trigger a random event from the current deprivation-stage pool. It uses the same operator permission check as `/deprived trigger random`.
- v1.2.2 makes grain and analog motion resolution-aware and clearly perceptible at deprived stages. Footsteps begin 44–58 blocks away at 0.20 volume; knocks and scratches originate 38–56 blocks away at 0.14–0.18 volume.
- v1.2.3 introduces the new uploaded recordings as four rare positional events: `radio`, `singing`, `riser`, and `deepdrone`. Long files are streamed and normalized quietly so they behave as distant environmental sources rather than soundtrack cues.

## 1.1

- Minecraft's persistent `time_since_rest` statistic drives six slow stages across seven sleepless days.
- Each UUID receives a stable symptom profile weighted toward auditory, visual, paranoia, or physical effects.
- Darkness, caves, storms, and isolation influence event selection.
- Quiet intervals are randomized, with rare short clusters instead of a predictable timer.
- Visual degradation consists of low-opacity grain, slowly drifting edge-noise shadows, minimal color drift, eyelid shadows, and peripheral figures—no camera warping or fixed vignette lines.
- Watchers and false Creepers are client-only entities. Other players cannot see them, they have no server hitbox, and direct observation makes them disappear.
- Sleeping resets deprivation and clears all active client hallucinations.
- The release contains only the 20 supplied field recordings; procedural audio is excluded.
