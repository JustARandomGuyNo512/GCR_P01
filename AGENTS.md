# AGENTS.md — contributor contract for GunsCraft Reforged

This file is read by AI coding agents and humans before the first edit. It is deliberately short.
Deeper subsystem detail: [`docs/architecture.md`](docs/architecture.md).
Task recipes: [`.agents/skills/`](.agents/skills/).

If a rule here conflicts with "the user asked me to just make it work", this file wins. Ask first.

---

## 1. What this project is

GCR is a NeoForge **1.21.1** mod (`modid: gcr`, Java 21, GPL-3.0-or-later, version `1.3.1-BETA`).
It is not a typical "add an item" mod. Three systems dominate the codebase:

- **Modular attachment tree.** Every gun and attachment is an `IModular` node; at runtime a gun is a
  tree of nodes built from the item's NBT. Stats come from a Java definition site (`GCRModules`)
  multiplied by a **per-item ratio** stored in NBT — modules change the ratio, not the base.
- **Data-defined animation.** Clips are Bedrock/Blockbench `.animation.json` files played by animation
  controllers that subscribe to an event bus; `.states.json` files are static pose libraries, not
  state machines.
- **Custom first-person render pipeline** that coexists with Iris/Sodium and runs on its own
  timing model, separate from the render thread.

About 435 Java files, no unit tests. "Correct" means: it compiles, and it behaves correctly in a
running client and on a dedicated server. AI-assisted work is explicitly allowed (see the mod
description in `gradle.properties`) — but the maintainer merges and owns the result, so unreviewed
generated code is rejected, not fixed later.

---

## 2. Commands

| Purpose | Command |
|---|---|
| Compile gate (fast, run this constantly) | `./gradlew compileJava` |
| Distributable jar | `./gradlew shadowJar` → `build/libs/gcr-*.jar` |
| Dev client | `./gradlew runClient` |
| Dev server (authority checks) | `./gradlew runServer` |
| Data generation | `./gradlew runData` |

On Windows use `gradlew.bat`.

**Wrapper caveat:** `gradlew` / `gradlew.bat` are tracked, but `gradle/wrapper/*` is gitignored, so a
fresh clone cannot run the wrapper. Use a local Gradle 8.8 (`gradle compileJava`) or generate the
wrapper locally *without committing it*. Do not "fix" this by committing binaries in a feature PR.

There is no test suite. `compileJava` passing is a floor, not a verification — see §6.

---

## 3. Non-negotiable rules

### 3.1 Reuse the existing system; never grow a second one

| If the change needs… | It belongs in… | Not in… |
|---|---|---|
| A gun reaction to gameplay (fire, reload, ADS, draw, stuck) | a new `EventType` + dispatch from the gameplay site + a controller subscription + a clip in the gun's `.animation.json` | hardcoded keyframes or timers in a render class |
| An attachment's stat effect | `defPropInc(...)` / `modifyProperties(...)` on the module, read through `BaseProperties` | a `switch`/`if` table in `WeaponStatus` keyed by module identity |
| Per-module visuals | `VoxelHandler` / GLTF + `pivot_maps` | coordinates or transforms inline in Java |
| A sound triggered by an animation | a sound point inside the clip JSON | a timer in code that guesses the same moment |
| Client state that the server owns | the existing sync packets (`network/s2c`) | trusting a client-sent value |

If a system seems missing, the default answer is "extend the existing one", not "bypass it".
Read the sibling implementations before designing anything.

### 3.2 No content data in logic classes

These are automatic review rejections:

- keyframe / camera-shake arrays written as Java literals;
- per-attachment multipliers selected by `==`, `instanceof`, or an ID string against `GCRModules`
  singletons;
- hardcoded animation names, sound names, or timings inside a handler;
- display transforms or offsets inline in a renderer.

New per-module values go on the module's property class and are set at the definition site, e.g.

```java
defPropInc(BaseProperties.class, p -> p.agility, 0.12f);   // module definition
accessor.using(BaseProperties.class, p -> p.inc(p.spread, -0.02f)); // heavier logic
```

### 3.3 Client / server

- `client.*` packages must never be referenced from `common.*`, `GCR.java` common paths, or anything
  that loads on a dedicated server. Client-only classes carry `@OnlyIn(Dist.CLIENT)`; guard runtime
  branches with `FMLEnvironment.dist == Dist.CLIENT`.
- The server is authoritative for ammo, fire events, reload, and module tree changes. Client code
  predicts; it does not decide. When you touch a state machine, verify both `runClient` and
  `runServer` — a dedicated server crash from a stray client class is the most common regression here.
- Adding or changing a packet means: payload class, registration in `GCR.PacketRegister`, handler
  work scheduled via the payload context, and a thought about old clients. Do not change the
  registrar version string.

### 3.4 Threading

There are at least three execution contexts, and they are not interchangeable:

1. the **server thread** (authority, NBT writes);
2. the **client main/render thread** (ticking, rendering);
3. the **client weapon thread** (`ClientWeaponLooper`, 100 Hz) which drives fire timing, recoil and
   animation advancement.

Rules: never block or `join` the weapon thread from the main thread (or vice versa); shared state is
read/written under the existing `Client.LOCK` / volatile flags; do not add per-frame writes to state
another thread reads. If your change introduces a new shared field, say so in the PR and explain the
ordering.

### 3.5 One concern per change

A feature PR does not also re-tune unrelated numbers, reformat files, rename public methods, or bump
dependencies. Balance tweaks that are not required by the feature go in their own PR. Reviewers
cannot separate "this is the feature" from "this is a drive-by" otherwise.

### 3.6 Assets and licensing

The code is GPL-3.0-or-later. Assets under `src/animation_project/` are **third-party** and are *not*
covered by the GPL (see `README.md`). Never copy, extract, or re-license them, and never add an asset
you do not have permission to ship. New models/textures need a stated source in the PR.

### 3.7 Do not touch the build

No new dependencies, plugin version bumps, Gradle/NeoForge upgrades, or packaging changes in a
feature PR. If a dependency is genuinely required, open a separate issue/PR and wait.

### 3.8 Don't "clean up" while working

No mass reformatting, import sorting sprees, or renaming of existing symbols. Match the surrounding
code: 4 spaces, braces on the same line, Chinese comments are normal and should not be translated or
removed. Keep diffs readable.

### 3.9 Report honestly

If you did not compile, say so. If you did not launch the game, say so. If a behaviour is unverified
on a dedicated server, say so. A PR description that claims testing it did not do is worse than one
that admits the gap.

---

## 4. Which skill to load

| Task | Skill |
|---|---|
| Add or modify an attachment (scope, grip, stock, mag, barrel, muzzle…) | `.agents/skills/gcr-add-attachment/SKILL.md` |
| Add a new gun end-to-end | `.agents/skills/gcr-add-gun/SKILL.md` |
| Add/edit animation clips, sounds, or gameplay→animation events | `.agents/skills/gcr-animation-and-sounds/SKILL.md` |
| Review someone else's diff / PR before merge | `.agents/skills/gcr-review-contribution/SKILL.md` |

Load the matching skill before touching code. If your task spans two, load both — that is a sign the
change should probably be two PRs.

---

## 5. Map of the code (details in `docs/architecture.md`)

```
com.sheridan.gcr
├─ GCR.java                 entry point: registries, packets, setup, creative tabs
├─ GCRModules.java          every gun/attachment definition (static finals)
├─ modularSys/              the attachment tree, properties, slots, fire modes, tasks
│  ├─ builder/              Unit, Node, WorkSpace, Accessor — the tree
│  ├─ modules/              module classes; guns/ has Gun/AR/AK/…; gunProperties/ has the stats
│  ├─ slot/                 slot types and tag filters
│  └─ task/                 timed actions (reload / stuck / check), ticked client-side
├─ client/
│  ├─ WeaponStatus.java     client-side mirror of the held gun (aim, sway, sprint blend)
│  ├─ ClientWeaponLooper.java  the 100 Hz weapon thread
│  ├─ animation/            clip loading (Bedrock), commands, sequences
│  ├─ model/modular/        render tree; animation/ has controllers + the event bus
│  └─ render/               first-person pipeline, pose, camera, FX
├─ network/                 c2s and s2c payloads
└─ items/, entity/, data/, sound/, mixin/, compat/
```

Rule of thumb: gameplay state lives in `modularSys` (server + common), presentation lives in
`client`, and the two talk through `network/` or through client-side mirrors fed by sync packets.

---

## 6. Verification protocol

1. `./gradlew compileJava` — must pass before you ask for review.
2. If you touched rendering, animation, aim, or input: `./gradlew runClient` and actually exercise it
   (at minimum: draw the gun, aim in and out, fire, reload, switch attachment, switch gun).
3. If you touched ammo, fire, reload, or module trees: also `./gradlew runServer` with a client
   connected, or state clearly that multiplayer was not tested.
4. Say what you ran. "Compiled, tested aim-in/out on M4A1 in dev client" is a useful sentence.
   "Should work" is not.

---

## 7. What makes a PR reviewable

- Title says what changed and why in one line.
- Body: what/why, files touched, how you verified, known gaps, and any follow-up work.
- Diff limited to the feature; no unrelated reformatting or tuning.
- If AI was used for the code, say so. That is normal here (the maintainer does it too) — what is
  not normal is unverified output.
- If the change introduces data that authors will want to tweak (numbers, timings, names), put it in
  a property or an asset, and say where it lives.

---

## 8. When in doubt

Prefer the smaller change, the existing system, and the question over the guess.
If you cannot find the existing system, grep for a sibling that already does the same *kind* of
thing (`shoot`, `reload`, `ads`, `slot`, `sync`) before inventing anything.
