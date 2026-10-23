---
name: gcr-review-contribution
description: Review a GunsCraft Reforged pull request or diff before merge — architecture-duplication red flags, GCR-specific correctness traps (client/server, threading, registration completeness), scope creep, and how to write the rejection or change request.
whenToUse: Use when asked to review a PR, judge whether a contribution is mergeable, or double-check a diff (including your own) before proposing it.
---

# Reviewing a contribution

The hard part of a GCR review is not "does it compile". Correct-looking code that grows a **second
implementation** of an existing system is the failure mode this project actually suffers from, because
AI-assisted contributors can produce a working feature much faster than they can learn the architecture.

Default question for every diff: *does this use the systems we already have, or does it quietly
duplicate one?*

---

## 1. Scope (fast pass)

```bash
git fetch origin pull/<n>/head
git diff --stat origin/main...FETCH_HEAD
git diff origin/main...FETCH_HEAD
```

Reject or split when:

- a feature PR also re-tunes unrelated numbers, reformats files, renames symbols, or bumps dependencies;
- the diff is much larger than the described change;
- build files, Gradle wrapper, NeoForge/Gradle versions or `build.gradle` dependencies moved.

Read **every** changed file. Descriptions lie by omission; diffs do not.

---

## 2. Architecture-duplication signals (the blockers)

Grep the diff for these. Each one means "the contributor built a parallel system":

| Signal in the diff | What it duplicates | Correct home |
|---|---|---|
| `static final float[][]` / `AIM_POS` / `catmull(` / hand-written interpolation | Bedrock clip pipeline + `KeyframeAnimator` | a clip in `*.animation.json` |
| `== GCRModules.X` / `instanceof ISight` / id-string `switch` inside `WeaponStatus`, `GunPoseHandler`, a renderer | attachment property system | `defPropInc` / `modifyProperties` |
| tick counters or `System.nanoTime` to fire a sound/animation moment | clip `sound_effects` / `timeline` | the animation JSON |
| per-module multipliers, magic numbers keyed by name | `BaseProperties` / `NumProp` | module definition |
| new `ComputeCameraAngles` handler, new `IGunRenderer`, second event bus | the single existing one | extend it |
| item NBT read in render code to learn recent state | event params (`SHOOT` carries `stuck`) | pass it in the event |
| a "fix" that deletes an existing general mechanism (e.g. stops calling `calculateFinalAimingSpeed`) while adding a special case | the general system | extend the general system |

Also check for **dead leftovers**: unused fields/methods added by the PR (e.g. a getter nothing calls,
a field written in two places with different meaning). That is the signature of an approach that was
abandoned mid-PR and not cleaned up — request the cleanup or reject.

A useful test: "in six months, could this be re-tuned or fixed by editing a definition or an asset,
without reverse-engineering the control flow around it?" If no, the data is in the wrong place.

---

## 3. GCR-specific correctness traps

### Registration completeness

For a new attachment: module definition, `GCR.java` item, creative tab entry, both lang files, item
model + icon, GLTF, voxel shape, `ClientTestingResources.registerModel`. Missing any one produces the
classic "added but invisible / unnamed / uninstallable" module.

For a new gun: all of the above plus `BaseProperties`, `DisplayData`, `RecoilData`, fire modes, slots
with filters **and** defaults, pivot map, clips, both animation mappings, controller `callInit*`, sounds.

### Client / server

- Any reference to `client.*` from a common path, from the `GCR` constructor, or from a common
  `@SubscribeEvent` is a dedicated-server crash waiting to happen.
- `@OnlyIn(Dist.CLIENT)` is not enforcement here — a call still fails at runtime.
- Ask explicitly: "did you run `runServer`?" A PR touching state machines that was only tested in the
  dev client is not verified.

### Threading

New shared fields must be `volatile` or `Client.LOCK`-protected. Blocking or joining across the weapon
thread (`ClientWeaponLooper`, 100 Hz) and the main thread deadlocks. Any change that adds per-frame
writes to state another thread reads is a rejection.

### Networking

- New packet: payload + `Codec` + registration in `GCR.PacketRegister`, all work inside
  `enqueueWork`, client-only types confined to `onClient`.
- `event.registrar("114514")` must not change.
- Authority: the server must re-derive from the held item and check `identityID`; a packet that
  carries an outcome the server should compute is a design problem (existing exceptions: the stuck
  roll, by design).

### Rendering

Own `MultiBufferSource`, `endBatch()` from a model, unrestored `RenderSystem` state, drawing during
the Iris shadow pass, or assuming `renderMode == true` are all blockers.

### Animation

Both mappings (json→registry, controller→registry) present? `coverState()` used where the clip takes
over bones? New event dispatched once per transition rather than per frame?

---

## 4. Verification claims

The PR body must state commands and actions actually performed. Acceptable: "compiled; dev client:
fired, reloaded, ADS in/out on M4A1". Not acceptable: "should work", "tested locally" with no detail,
or silence about the dedicated server when state machines changed.

If you cannot reproduce the manual test in reasonable time, say which part is unverified rather than
assuming it works.

---

## 5. Decision

| Verdict | When |
|---|---|
| **Merge** | compiles, played in client (and server if state changed), no duplicated system, diff legible, data in the right place |
| **Request changes** | correct approach but dead code, missing lang/icon, naming, docs, tests claimed but not done |
| **Reject** | duplicates an existing system, bypasses client/server or threading rules, bundles unrelated changes, or is unverified in a security/authority-sensitive path |

A rejection is about the diff, not the person. The idea can be good and the implementation still be
wrong for the codebase — say both.

---

## 6. Writing the review

Structure that works:

1. Thanks + one specific thing that is good (the test plan, the model work, the description).
2. "I read the full diff" — reviewers who say this get better follow-ups.
3. Two or three blockers, each with **file:line**, what it duplicates, and the system it should use
   instead. Do not dump ten nits; the contributor cannot act on ten.
4. A path forward: rework on top of X, or split the unrelated tweaks into their own PR.
5. Disclose if the message was AI-drafted (the maintainer's English is not the point; honesty is).

Avoid: "this is bad", "use best practices", or a list of style opinions. Every comment should name a
file and a concrete alternative.

---

## 7. Quick checklist

- [ ] One concern, no drive-by tuning/reformatting
- [ ] No parallel animation/property/networking implementation
- [ ] No dead code from an abandoned approach
- [ ] Registration complete (module/gun/asset/lang/tab/model)
- [ ] No `client.*` reachable from the server side
- [ ] Threading respected (volatile / `Client.LOCK` / no cross-thread joins)
- [ ] Packet registration + `enqueueWork` + unchanged registrar version
- [ ] Render state restored; no shadow-pass drawing
- [ ] Data lives where it can be tuned without Java
- [ ] Verification actually performed and stated
