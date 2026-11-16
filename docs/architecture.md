# GCR architecture map

Structural map for contributors and AI agents. It answers "where does this live, and what must stay
true when I change it" — not "how does every method work". When this file and the code disagree, the
code wins; fix the file.

Pairs with [`AGENTS.md`](../AGENTS.md) (rules) and [`.agents/skills/`](../.agents/skills/) (recipes).

---

## 0. Load order (why static initialisation matters here)

```
GCR class-init
  └─ first GCRModules.X reference  →  GCRModules class-init
        └─ 81 × `public static final IModular`   →  Module ctor → ModuleRegister.register(id, this)
              (duplicate id ⇒ IllegalArgumentException at class-init: mod fails to load)
  └─ ITEMS.register(...) enqueues suppliers only
FMLCommonSetupEvent → GCR.commonSetup: for each ModuleItem → book pivot/voxel assets → module.finalizeInit()
FMLClientSetupEvent → ClientTestingResources: gltf load + registerModel(...) + animation registration
```

Consequences that bite:

- A module registers itself when its class field is initialised. Referencing a module from an
  unrelated static initialiser can register it earlier than expected.
- `finalizeInit()` freezes tags into an `ImmutableSet`; `addTags` after that silently does nothing.
- Only **item-bound** modules are walked in common setup. A module without a `ModuleItem` never gets
  its assets booked, and the modify screen will NPE on `getBindItem()`.
- `ModuleRegister.finalizeRegister()` exists but is never called, so late registration is not blocked.

---

## 1. The attachment tree

| Type | File | Role |
|---|---|---|
| `IModular` | `modularSys/IModular.java` | Contract: id, tags, properties, validate, hover text |
| `Module` | `modularSys/modules/Module.java` | Self-registering base; weight, tags, fixed position |
| `Unit` | `modularSys/builder/Unit.java` | A tree node's payload (module + z offset + render params). Identity semantics, no `equals` |
| `Node` | `modularSys/builder/Node.java` | Unit + parent + slot children; `dfs` is pre-order |
| `WorkSpace` | `modularSys/builder/WorkSpace.java` | Editable tree: add/remove/replace child, serialise to NBT |
| `ShadowNode` | `modularSys/builder/ShadowNode.java` | Immutable snapshot for read-only consumers |
| `SlotInstance` | `modularSys/builder/SlotInstance.java` | A slot as instantiated on one node; per-instance hidden flag |
| `ISlot` / `SlotFilters` | `modularSys/slot/` | Fitting rules: tag filters, capacity, add/remove/replace permission |

Mental model: a module *declares* slots (`ISlotProviderModular`); each node instantiates those slots;
children hang off `(node, slot)` pairs. The same module definition can appear in many guns — the tree
owns position and state, not the module.

Rules that hold:

- `Slot.accepts(m)` = `filter == null ? true : filter.test(m)` — **an unfiltered slot accepts anything**.
  Fit is expressed entirely through tags (`SlotFilters.hasAllTags(...)`, `hasTag(...)`, `modular(id)`).
- Tag vocabulary is flat strings (`sight`, `scope`, `upper`, `on_rail`, `m_lok_rail_fit`, `ar`, `ak`,
  `5.56x45`, …). Another module's `==` on tags is a code smell; use `hasTag`.
- Node ids are derived from `parent + slot + module + index` and are **remapped on every edit**. Never
  persist a node id across a mutation.
- `renderParams` and `SlotInstance.hidden` are flushed before every `onMutated` hook; anything custom
  must be re-emitted on each mutation.

---

## 2. Stats (properties)

`modularSys/modules/gunProperties/`:

- `Properties` / `BaseProperties` — the stat sheet: `rpm, recoilControl, stability, weight, spread,
  stuckRate, maxStuckRate, agility, impulse, aimingSpeed, fireSoundRange, bulletVelocity,
  shootHeatInc, heatDecSpeed, baseDamage`, plus fire sound names and reload/clip timers.
- `NumProp` — **Java base value × NBT ratio**, where the ratio lives in the item's `CUSTOM_DATA`
  (clamped only when `inc`/`dec` writes it, not on read). `inc`/`dec` change the ratio; modules add
  their effect there. `AccProp` (weight) is the exception: it stores an absolute value.
- `PropertiesAccessor.using(BaseProperties.class, p -> …)` — the only sanctioned way for a module to
  touch stats (`defPropInc(...)` on module base classes is the sugar for it).

Implications:

- The **base number is a Java literal in `GCRModules`**; there is no data-driven stat file. Changing
  balance means editing the definition and restarting — a server start rebuilds item data anyway.
- Balance numbers belong at the definition site or in a property default — never in a renderer, and
  never as a table keyed by module identity.
- Because the ratio is per item, two copies of the same gun can differ; code must read properties
  from the stack, not from the module.
- `Gun.reCalculateProperties` recomputes from the tree, so anything that modifies stats outside the
  property system is lost on the next recompute (including on the next server start).
- Aim speed is the one stat with an extra layer: `WeaponStatus` keeps the base value and multiplies it
  by the active sight's `ISight.getAdsSpeedModifier()` (`refreshAimingSpeed()`), so swapping optics
  changes how fast ADS completes without recomputing the stats.

---

## 3. Fire, tasks, reload

- Fire modes: `modularSys/fire/*` (`IFireMode`, `ARFullAuto`, `ARSemi`, `AKFullAuto`, `AKSemi`, …).
  They own the client intent (`clientShoot`) and the server-side consumption (`useAmmo`).
- Long actions are **tasks**: `modularSys/task/` (`IGunTask`, `GunTaskHandler`, reload / stuck / check
  tasks). A task is ticked by the client and drives the animation; the server only accepts the
  resulting state.
- Reload timing comes from the **clip length**, not from a constant: the task runs the animation and
  sends `GunReloadPacket` when the clip says the mag is seated. `BaseProperties.taskTimers` carries the
  per-clip lengths (`mag_reload_length`, …) used as the packet delay. A longer reload means editing the
  clip and its `taskTimers` entry — there is no clip-pool/random-variant mechanism in the code.

---

## 4. Animation

Pipeline (see `.agents/skills/gcr-animation-and-sounds/` for the full recipe):

```
gameplay code → Client.getGunRenderer().dispatchAnimationEvent(EventType, params)
              → DefaultGunRenderer (queues if no context yet)
              → AnimationEventBus.dispatch → controller callback
              → Track.play(anim("clip").coverState()) → KeyframeAnimator
              → bones + sound_effects + timeline commands
```

| Piece | File |
|---|---|
| Event types | `client/model/modular/animation/eventSys/EventType.java` |
| Bus | `.../eventSys/AnimationEventBus.java` |
| Base subscriptions | `client/model/modular/animation/controllers/GunController.java` |
| Per-gun controllers | `.../controllers/ARMainController.java`, `AKController.java`, … |
| Clip loading | `client/animation/io/BedrockAnimationLoader.java` |
| Clip registry | `client/animation/AnimationRegister.java` |
| Timeline commands | `client/animation/command/CommandFactory.java` |
| Wiring | `ClientTestingResources.java` |

- Clips are Bedrock `.animation.json` (`assets/gcr/model_assets/animation/`), with `sound_effects` and
  `timeline` embedded — sound cues and logic live in the data, not in `tick()`.
- Two mappings must exist for a clip to play: json key → registered name
  (`loadAndRegisterAnimations`) and controller key → registered name (`registerAnimations`). Missing
  either throws `Animation not found: <key>`.
- `.states.json` is a **static pose library** (bone visibility/scale per gameplay state), consumed by
  the viewers, not by controllers. It is not a state machine.
- Timing: clips advance on wall-clock time, sound cues are quantised to the 20 Hz client tick, and the
  JSON is read once at client setup — editing a clip requires a restart, not a resource reload.
- There is no ADS event type yet; aim changes are driven from `WeaponStatus` / `GunPoseHandler`.

---

## 5. First-person rendering

```
RenderItemMixin → IGunRenderer.renderFirstPerson (DefaultGunRenderer)
  → cache check by identityID/modifyID → pose pass (AnimationHandler, HardCodeAnimationHandler,
    GunPoseHandler, RecoilHandler) → FirstPersonRenderContext.startRender()
  → calcPose() authors BoneRenderStatus per bone → BufferedBoneMeshModel draws static VBO + bone UBO
  → FX quads go through context.getBuffer(...) into FP_BUFFER_SOURCE; DefaultGunRenderer flushes
```

Ownership table:

| Effect | Owner |
|---|---|
| Gun pose / ADS blend | `client/render/GunPoseHandler.java` |
| Camera rotation from the `camera` bone | `client/animation/CameraAnimationHandler.java` (set **every** frame; it clears itself) |
| Sway, bob, sprint, shoot resistance | `client/render/HardCodeAnimationHandler.java` |
| Camera recoil | `client/recoil/RecoilCameraHandler.java` |
| ADS FOV / zoom | `WeaponStatus` + `RenderEvents.handleScope` |

Hard constraints:

- Never take your own `MultiBufferSource`; use `context.getBuffer(...)`.
- Never call `endBatch()` from a model (the renderer owns flushing; the per-render-type exception is
  documented in `MuzzleFlashRenderer`).
- Never draw during the Iris shadow pass (`IrisCompat.isRenderingShadowPass()`).
- Pose is computed once per frame; `BoneRenderStatus` must not be mutated between `calcPose` and draw.
- Under Iris the FP stack is scaled `0.25`; new effects in gun-local units need the 4× correction.
- Cache invalidation is by `identityID`/`modifyID`; if you add state derived from the module tree,
  hook `invalidateRenderCache()`.
- `renderMode == false` is a pose-only pass (used by `GunPoseHandler.calculateSightPose`); do not
  assume arms/shells/camera run.

---

## 6. Networking and authority

Payload pattern: `network/c2s|s2c/*Packet implements CustomPacketPayload, IPacket<Self>`, a
`STREAM_CODEC` built from the private `encode`/`decode`, registration in `GCR.PacketRegister` via
`playBidirectional(TYPE, STREAM_CODEC, new DirectionalPayloadHandler<>(onClient, onServer))`.
Registrar version string `"114514"` — do not change it.

| Direction | Packets |
|---|---|
| C2S | `GunFirePacket`, `GunReloadPacket`, `SwitchUsingSightPacket`, `RemoveStuckPacket`, `SubWeaponFirePacket`, `SubWeaponReloadPacket`, `SyncGunStatusPacket`, `SyncPlayerStatusPacket`, `CommitModuleTreePacket`, `PlaySoundPacket` |
| S2C | `GunFireAckPacket`, `GunStuckSyncPacket`, `CommitModuleTreeResponsePacket`, `InitClientGunDataPacket`, `BroadcastPlayerStatusPacket`, `BroadcastLivingFirePacket` |
| Dead | `SyncHeatDataPacket` (deprecated, unregistered) |

Authority, as actually implemented:

| State | Owner | Notes |
|---|---|---|
| Module tree, properties, states | server (item NBT) | full pushes on equip/commit; client mirrors |
| Ammo / chamber | server | client predicts locally; ack carries authoritative `ammoLeft` |
| Bullets / fire events | server | client predicts flash/animation/recoil; every shot gets an ack, including rejections |
| Stuck roll | **client decides**, server applies | deliberate: animation timing + phantom-shot avoidance |
| Reload | server | weak validation: no timing/ability check, the client task decides *when* |
| Aiming / ADS | client only | not replicated |
| Attachment changes | server | strongest path: rebuild, inventory check, validate, revert on failure |

The codebase rule is **re-derive + identity check**: handlers start from `player.getMainHandItem()`,
compare the packet's gun id with the current `identityID`, and recompute consumption. A new packet
should do the same rather than trusting its payload.

---

## 7. Threading

Three contexts:

1. **Server thread** — authority, NBT writes.
2. **Client main thread** — ticking and rendering (the same thread in Minecraft).
3. **Client weapon thread** — `ClientWeaponLooper`, 100 Hz, drives the fire timeline, recoil and
   animation advancement.

Rules:

- The main thread holds `Client.LOCK` for the whole client tick; the weapon thread takes it only when
  actually firing, and must never wait on the main thread while holding it.
- Fields shared with the weapon thread are `volatile` or lock-protected (`WeaponStatus.fireBlocked`,
  `ClientGunStuckCache` entries).
- Network handlers run off-thread and must hop through the payload context's `enqueueWork`.
- Do not read item NBT on the render thread to learn what the weapon thread just did — pass it as an
  animation event parameter instead (that is why `SHOOT` carries `stuck` / `last_round`).
- GL resources are created/destroyed on the render thread only (`RenderSystem.recordRenderCall`).

---

## 8. Client/server split

- `client.*` must not be reachable from code that loads on a dedicated server. Registration that both
  sides need lives in common packages (`ModContainers`, `ModParticles` are deliberately
  package-misleading but side-safe).
- `@OnlyIn(Dist.CLIENT)` is documentation in this build, not enforcement — a call still crashes at
  runtime. Guard with `FMLEnvironment.dist == Dist.CLIENT` / `Dist.CLIENT` event subscribers.
- Landmines to respect:
  - `Client` lives in the *common* package but its static initialiser builds renderer objects.
    Touching `Client` from server code is fatal; keep it behind client-only call paths.
  - `IrisShaderCompatMixin` is declared in the **common** mixin list but targets Iris by string
    (`@Mixin(targets = "net.irisshaders.iris.pipeline.transform.TransformPatcher")`). The string target
    is what keeps it inert without Iris — do not "clean it up" into a class literal.
  - `GCR` imports client classes; they are only safe inside `Dist.CLIENT` subscribers/branches.

---

## 9. Assets layout

```
src/main/resources/assets/gcr/
├─ lang/en_us.json, zh_cn.json          "gcr:<id>" display names, tooltips
├─ models/item/<id>.json                inventory model (parent item/generated)
├─ textures/item/<id>.png               inventory icon
├─ model_assets/gltf/<id>.gltf + .png   in-world mesh
├─ model_assets/animation/*.animation.json   Bedrock clips per gun
├─ model_assets/animation/*.states.json      static pose library (not a state machine)
├─ common/voxel_shapes/*.geo.json       collision / fit geometry (both sides)
├─ common/pivot_maps/*.pivot.geo.json   child-slot pivots (both sides)
└─ sounds.json + sounds/<family>/*.ogg  sound events
```

`src/animation_project/` holds Blockbench sources and third-party assets (not GPL — see `README.md`).

---

## 10. Known rough edges (read before "fixing" them)

Structural issues:

- The Gradle wrapper jar is gitignored; a fresh clone cannot run `./gradlew`.
- `ModuleRegister` has an unused `finalizeRegister()`; registration is not actually frozen.
- The `Config` options are template leftovers and are read by nothing; there is no client config.
- `SyncGunStatusPacket` copies every client-sent key into node states without a whitelist.
- `PlaySoundPacket` lets the client name a sound and position without validation.
- `SightViewRenderer` is a stub; `Client.handleWeaponBobbing` and `ModuleRenderContext.bufferCache` are
  unused; `SyncHeatDataPacket` is dead.
- Two delayed-render mechanisms coexist (`Stage` and `RenderEvents.addDelayed*`); use `Stage`.
