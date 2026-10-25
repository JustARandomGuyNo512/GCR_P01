---
name: gcr-add-attachment
description: Add a new attachment (scope, red dot, grip, stock, magazine, barrel, muzzle, handguard) to GunsCraft Reforged, or change what an existing attachment does. Covers module definition, item + creative tab registration, GLTF/voxel assets, model registration, language keys, and stat wiring through the property system.
whenToUse: Use when the task is "add an attachment / accessory / optic / grip / stock / mag", or when an existing attachment has the wrong effect, is missing from the creative tab, cannot be installed, or renders invisibly.
---

# Adding an attachment

An attachment is an `IModular` module. It has to be registered in several places — definition, item,
creative tab, language, icon, voxel, mesh, client model. Missing any one produces the classic
half-added module (it exists, but is invisible / unnamed / uninstallable).

Read `AGENTS.md` §3.1–3.2 first. If you are about to add a `switch` on module identity in
`WeaponStatus`, delete it and put the number on the module instead.

---

## 0. Find the sibling to copy

Do not design a new module class unless nothing fits. Existing implementations:

| Category | Base class (in `modularSys/modules/impl/`) | Example in `GCRModules` |
|---|---|---|
| Scope / red dot | `Scope`, `RedDot` (both `ISight`) | `ACOG`, `VORTEX_RAZOR_RED_DOT` |
| Pistol / rear grip | `RiflePistolGrip` | `MOE_GRIP` |
| Forward grip | `ForwardGrip` | `KAC_FORWARD_GRIP` |
| Stock | `Stock` | `MOE_CARBINE_STOCK` |
| Magazine | `Mag` | `STANAG_MAG_30R` |
| Muzzle device | `Muzzle` (suppressor flag matters) | `a2_flash_hider` family |
| Barrel / handguard | `ARBarrel`, `Handguard`, … | `M4_PROFILE_FSB_BARREL` |
| Anything with custom logic | `UniqueModule` / `AttachmentModule` | — |

All of them extend `Module`, which **registers itself in its constructor**. Two modules with the same
id throw `failed to register module: …` at class-init time. Never copy a definition without changing
`RL("…")`.

Two API details that break new contributors:

- `addTags(...)` exists on `Module`, not on `IModular`. Chain it on the concrete type
  (`new Scope(...).addTags(...)`); assigning to an `IModular` variable first and calling `addTags`
  does not compile.
- Modules are **identity**, not value, objects: `Unit`, `WorkSpace.unitToNode`, `ModuleModelRegister`
  and `ModelRegistrationManager.MODEL_RECIPES` are all keyed by instance. Never construct a second
  `new Scope(...)` to "look one up" — it registers a duplicate id and owns no model.

---

## 1. Define it in `GCRModules.java`

`src/main/java/com/sheridan/gcr/GCRModules.java` holds every definition as a `public static final IModular`.
Put the new one next to its category, not at the end of the file.

```java
public static final IModular MY_SCOPE = new Scope(
        RL("my_scope"),
        new VoxelHandler(RL("common/voxel_shapes/my_scope_voxel.geo.json")),
        0.3f,    // weight
        0.88f,   // ADS speed modifier: multiplied into aim speed while this sight is active
        4f, 4f,  // min / max magnification
        0.15f    // zoom sensitivity
).addTags("sight", "scope", "upper", "on_rail");
```

Two things decide whether the module can be installed at all:

- **Tags.** Slots filter with `SlotFilters.hasAllTags("sight", "upper", "on_rail")` and friends
  (`ar`, `ak`, `5.56x45`, `m_lok_rail_fit`, `stock`, `mag`, `barrel`, `muzzle`, `handguard`…). Grep
  the gun definition's `.addSlot(new ...Slot("…").setFilter(...))` to see what it requires. A tag
  that no slot accepts makes the module unusable; a missing family tag (`ar`/`ak`) makes it fit
  nothing.
- **Constructor data.** Weight, multiplier, magnification, and sound/visual parameters are
  constructor arguments on the module class. That is the intended data channel.

For stat effects, use the property system, never a lookup table elsewhere:

```java
defPropInc(BaseProperties.class, p -> p.agility, 0.12f);          // on a grip
accessor.using(BaseProperties.class, p -> p.inc(p.spread, -0.02f)); // in modifyProperties(...)
```

Two channels exist and both are legitimate: **persisted** (`defPropInc` on a `NumProp` ratio — shows in
the tooltip automatically, survives rebuilds) and **held-only**
(`IArmHandlerModular.AdditionalPropModifier` — added at query time for recoil/stability/agility/aim
speed). Pick per the sibling module's behaviour.

Optics additionally carry an `adsSpeedModifier` constructor argument, which `WeaponStatus` multiplies
into the aim speed while that sight is active (`refreshAimingSpeed`). It is a *speed* multiplier, not a
zoom setting, and it is separate from the canted-sight 0.75/0.6 pacing factors applied by the same
class.

`BaseProperties` (`modularSys/modules/gunProperties/impl/BaseProperties.java`) is the stat sheet:
`rpm, recoilControl, stability, weight, spread, stuckRate, maxStuckRate, agility, impulse,
aimingSpeed, fireSoundRange, bulletVelocity, shootHeatInc, heatDecSpeed, baseDamage`.
(`bulletVelocity` is the projectile speed in blocks/tick, read by `Gun.serverShoot`.)

---

## 2. Register the item and the tab entry

`src/main/java/com/sheridan/gcr/GCR.java`, in the `DeferredItem` block:

```java
public static final DeferredItem<Item> MY_SCOPE_ITEM =
        ITEMS.register(GCRModules.MY_SCOPE.getSimpleID(), () -> new ModuleItem<>(GCRModules.MY_SCOPE));
```

and add `output.accept(MY_SCOPE_ITEM.get());` inside `ATTACHMENT_TAB`'s `displayItems`.
Items that are not listed there are unreachable in survival/creative testing.

---

## 3. Inventory icon — name it in both languages

Two assets plus two lang entries. Missing either asset renders a missing-texture cube, which is the
most common "it works but looks broken" report.

```
assets/gcr/models/item/<id>.json      {"parent":"item/generated","textures":{"layer0":"gcr:item/<id>"}}
assets/gcr/textures/item/<id>.png     16x16 icon
```

`src/main/resources/assets/gcr/lang/en_us.json` **and** `zh_cn.json`:

```json
"gcr:my_scope": "My Scope"
```

The key is `gcr:<simple id>` (it comes from `ModuleItem.getDescriptionId() → module.getID()`), not an
`item.` prefix. If you add a new tooltip line, add its key to both files too (`tooltip.prop.*` pattern).

---

## 4. Give it a model

A module without a model registration is invisible in game; a module without a voxel shape is
collision-free. These are separate layers with separate failure modes:

| Asset | Location | Required when | Failure if missing |
|---|---|---|---|
| Mesh | `assets/gcr/model_assets/gltf/<id>.gltf` + `.png` | always | invisible on the gun |
| Voxel shape | `assets/gcr/common/voxel_shapes/<id>_voxel.geo.json` | always (collision/fit, **both sides**) | installs but clips / no collision |
| Pivot map | `assets/gcr/common/pivot_maps/<id>.pivot.geo.json` | module **provides child slots** | children mis-positioned / collision skipped |
| Registration | `src/main/java/com/sheridan/gcr/ClientTestingResources.java` | always | invisible on the gun |

```java
ModelRegistrationManager.registerModel(
        GCRModules.MY_SCOPE, "model_assets/gltf/my_scope.gltf", "model_assets/gltf/my_scope.png", true,
        meshData -> new ScopeModel(meshData, GCR.RL(""), /* … */));
```

There is currently a 1:1 relationship: 81 modules, 81 `registerModel` calls, 81 `models/item/*.json`,
81 `textures/item/*.png`. Use the sibling's model class (`ScopeModel`, `RedDotModel`, generic modular
model) — do not invent a model class for a mesh that an existing one already renders.

**Bone names are API.** A module that provides a slot (`SCOPE`, `MUZZLE`, `MAG`, …) must have a bone of
that exact name in its GLTF; `ModuleRenderNode.addSlot` silently drops children whose slot has no bone,
so the attachment installs and does nothing. Same for `ScopeModel` (`REAR_LENS`, `CROSS_HAIR`,
`CROSS_HAIR_Z`) and the M-LOK handler (voxels named `m_lok` and `picatinny`, both required, missing ⇒
hard throw).

Voxel shapes are Bedrock geometry `1.12.0` (`minecraft:geometry` → `bones` → `cubes` with
`origin/size/uv`, 1/16 units). Authoring constraints enforced in code: `VoxelLoader` keeps only bones
named/parented `root` and **ignores rotated cubes**; `Rail` / `MLokRail.of` throw at class-init if the
geometry ranges are inconsistent (a bad rail is a mod-load crash, not a log line).

---

## 5. Verify (do not skip)

1. `./gradlew compileJava`.
2. `./gradlew runClient` → the attachment appears in the **Attachments** tab, with the right icon and
   name, and can be installed on the intended gun (and is rejected on the others).
3. Hovering the module shows the expected stat change (the property tooltip is generated from
   `AdditionalPropModifier` / `Scope.appendHoverText`).
4. It renders on the gun in first person, at the right offset, and does not clip through the handguard.
5. `./gradlew runServer` boots — module classes are constructed in common setup, so a client-only
   import in a module class crashes a dedicated server.

Report exactly which of these you ran.

---

## Hard "no"s

- Adding a per-module multiplier table in `WeaponStatus` / `GunPoseHandler` (this is the single most
  common rejected contribution).
- Hardcoding the model transform in a renderer instead of using the model registration.
- Adding tags that no slot filter accepts, or forgetting the family tag.
- Reusing an existing module id.
- Editing `WeaponStatus` to special-case one attachment instead of setting a property.
- Adding assets whose license you cannot state (see `AGENTS.md` §3.6).

## PR checklist

- [ ] Definition in `GCRModules` with unique id and correct tags
- [ ] `GCR.java` item + `ATTACHMENT_TAB`
- [ ] `en_us.json` + `zh_cn.json`
- [ ] GLTF/PNG (+ voxel/pivot if needed) and `ClientTestingResources` registration
- [ ] Stats via properties/constructor, not a lookup table
- [ ] `compileJava` + dev-client install test, stated in the PR body
