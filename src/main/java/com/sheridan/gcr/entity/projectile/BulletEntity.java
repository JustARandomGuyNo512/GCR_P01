package com.sheridan.gcr.entity.projectile;

import com.sheridan.gcr.damageTypes.ModDamageTypes;
import com.sheridan.gcr.entity.projectile.PlayerAnatomy;
import com.sheridan.gcr.config.PlayerHitZoneConfig;
import com.sheridan.gcr.compat.OwnMultipartHitFilter;
import com.sheridan.gcr.network.GCRNetwork;
import com.sheridan.gcr.network.s2c.BulletImpactPacket;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.entity.PartEntity;
import com.sheridan.gcr.modularSys.modules.guns.Gun;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class BulletEntity extends Entity {
    private long serverBirthTime;
    private LivingEntity shooter;
    private Gun gun;
    // 新增：命中后先移动到命中点，延迟一帧再真正移除，避免客户端来不及渲染就消失
    private boolean markedForRemoval = false;
    public static final EntityDataAccessor<Vector3f> EXACT_VELOCITY =
            SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.VECTOR3);
    private int shooterId;
    public float baseDamage;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public void setBaseDamage(float baseDamage) {
        this.baseDamage = baseDamage;
    }

    public void setShooter(LivingEntity shooter) {
        this.shooter = shooter;
        this.shooterId = shooter.getId();
    }

    public void setGun(Gun gun) {
        this.gun = gun;
    }

    private static final Vector3f ZERO_VELOCITY = new Vector3f(0.0F, 0.0F, 0.0F);

    @Override
    protected void defineSynchedData() {
        this.entityData.define(EXACT_VELOCITY, ZERO_VELOCITY);
    }

    @Override
    public void setDeltaMovement(@NotNull Vec3 velocity) {
        super.setDeltaMovement(velocity);
        if (!this.level().isClientSide) {
            if (markedForRemoval) {
                this.getEntityData().set(EXACT_VELOCITY, ZERO_VELOCITY);
            } else {
                this.getEntityData().set(EXACT_VELOCITY, velocity.toVector3f());
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (EXACT_VELOCITY.equals(key) && this.level().isClientSide) {
            Vector3f exactVec = this.getEntityData().get(EXACT_VELOCITY);
            super.setDeltaMovement(new Vec3(exactVec.x(), exactVec.y(), exactVec.z()));
        }
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            Vector3f vector3f = this.getEntityData().get(EXACT_VELOCITY);
            Vec3 start = position();
            Vec3 end = start.add(vector3f.x, vector3f.y, vector3f.z);
            setPos(end);
            return;
        }

        // 新增：上一 tick 已经命中过，这一 tick 才真正移除，
        // 保证命中那一帧的位置同步包已经发给了客户端
        if (markedForRemoval) {
            discard();
            return;
        }

        if (System.currentTimeMillis() - serverBirthTime > 2500) {
            discard();
            return;
        }
        Vec3 start = position();
        Vec3 velocity = getDeltaMovement();
        Vec3 end = start.add(velocity);

        BlockHitResult blockHit = level().clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ) {
            @Override
            public @NotNull VoxelShape getBlockShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
                if (state.is(BlockTags.LEAVES)) {
                    return Shapes.empty();
                }
                return super.getBlockShape(state, level, pos);
            }
        });

        Vec3 entityCheckEnd = end;
        if (blockHit.getType() != HitResult.Type.MISS) {
            entityCheckEnd = blockHit.getLocation();
        }

        // Search broadly but resolve an impact ONLY against the actual target
        // geometry. The former ProjectileUtil raycast added an invisible margin.
        ResolvedContact entityHit = findExactEntityHit(start, entityCheckEnd, velocity);
        // Server-authoritative visual water trail, ending at the nearest
        // actual collision. This does not modify GCR bullet physics or damage.
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 trailEnd = entityHit != null ? entityHit.hit().getLocation() : entityCheckEnd;
            GcrBulletWaterEffects.trace(serverLevel, start, trailEnd);
        }

        if (entityHit != null) {
            onHitEntity(entityHit);
            return;
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            onHitBlock(blockHit);
            return;
        }

        setPos(end);
    }

    /** The coarse search expansion never changes the actual hitbox. */
    private ResolvedContact findExactEntityHit(Vec3 start, Vec3 end, Vec3 velocity) {
        AABB candidates = getBoundingBox().expandTowards(velocity).inflate(1.0);
        double nearestSq = start.distanceToSqr(end);
        ResolvedContact nearest = null;
        for (Entity target : level().getEntities(this, candidates,
                e -> e != this
                        && !OwnMultipartHitFilter.isOwnerOrOwnedPart(e, shooter)
                        // Player anatomy is resolved ONLY through PlayerAnatomy.
                        // Do not let Aftermatch/other player-part entities bypass
                        // that path and receive ordinary entity damage instead.
                        && !(e instanceof PartEntity<?> part && part.getParent() instanceof Player)
                        && e.isPickable() && e.isAlive())) {
            Vec3 intersection;
            PlayerAnatomy.Part part = null;
            if (target instanceof Player player) {
                // Only players receive anatomical hitboxes or location damage.
                PlayerAnatomy.Hit anatomical = PlayerAnatomy.trace(player, start, end);
                if (anatomical == null) continue;
                intersection = anatomical.location();
                part = anatomical.part();
            } else {
                AABB exact = target.getBoundingBox();
                intersection = exact.contains(start)
                        ? start : exact.clip(start, end).orElse(null);
                if (intersection == null) continue;
            }
            double distanceSq = start.distanceToSqr(intersection);
            if (distanceSq < nearestSq) {
                nearestSq = distanceSq;
                nearest = new ResolvedContact(
                        new EntityHitResult(target, intersection), part);
            }
        }
        return nearest;
    }

    private record ResolvedContact(EntityHitResult hit, PlayerAnatomy.Part playerPart) {}

    private void onHitBlock(BlockHitResult hit) {
        Level world = this.level();
        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
            BlockPos pos = hit.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Direction direction = hit.getDirection();
            Block block = state.getBlock();

            if (interactWith(pos, state, direction, block, hit)) {
                SoundType soundType = state.getSoundType();
                boolean isMetal = soundType == SoundType.METAL || soundType == SoundType.NETHERITE_BLOCK;
                if (isMetal) {
                    world.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.8F);
                } else {
                    world.playSound(null, pos, soundType.getBreakSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
                }
                Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
                double speed = 2.5f;
                double vx = normal.x * speed;
                double vy = normal.y * speed;
                double vz = normal.z * speed;
                BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, state);
                Vec3 hitLocation = hit.getLocation();
                // Cosmetic only. PlayerAnatomy and PlayerHitZoneConfig remain
                // authoritative for player hits and bullet damage.
                GCRNetwork.CHANNEL.send(
                        PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                                hitLocation.x, hitLocation.y, hitLocation.z, 96.0, serverWorld.dimension())),
                        new BulletImpactPacket(hitLocation, direction));
                for (int i = 0; i < 8; i++) {
                    double rx = vx + (world.random.nextDouble() - 0.5) * 0.25;
                    double ry = vy + (world.random.nextDouble() - 0.5) * 0.25;
                    double rz = vz + (world.random.nextDouble() - 0.5) * 0.25;
                    serverWorld.sendParticles(
                            particleData,
                            hitLocation.x, hitLocation.y, hitLocation.z,
                            0,
                            rx, ry, rz,
                            2
                    );
                }
            }
            if (tickCount < 2) {
                markedForRemoval = true;
            } else {
                discard();
            }
        }

        setPos(hit.getLocation());
    }

    protected boolean interactWith(BlockPos pos, BlockState state, Direction direction, Block block, BlockHitResult result) {
        boolean isPlayerShooting = this.shooter instanceof ServerPlayer serverPlayer;
        if (!isPlayerShooting) {
            return true;
        }
        if (block instanceof BellBlock bell) {
            bell.attemptToRing(null, this.shooter.level(), pos, direction);
            return false;
        }
        return true;
    }


    private void onHitEntity(ResolvedContact contact) {
        EntityHitResult hit = contact.hit();
        Entity target = hit.getEntity();
        target.invulnerableTime = 0;
        DamageSource damageSource = createBulletDamageSource();
        float damage = (float) (baseDamage * (0.9f + 0.2f * Math.random()));
        // Player-only anatomical damage. Mobs and other entities retain the
        // exact same damage as before. Armor is still handled by vanilla hurt().
        if (target instanceof Player && contact.playerPart() != null) {
            damage *= PlayerHitZoneConfig.multiplier(contact.playerPart().zone());
        }
        target.hurt(damageSource, damage);
        setPos(hit.getLocation());
        if (tickCount < 2) {
            markedForRemoval = true;
        } else {
            discard();
        }
    }

    private DamageSource createBulletDamageSource() {
        Holder<DamageType> bulletType = level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageTypes.BULLET);
        Entity causingEntity = shooter == null ? this : shooter;
        return new DamageSource(bulletType, this, causingEntity) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity victim) {
                Component gunName = gun == null
                        ? Component.translatable("gcr:unknown_gun")
                        : Component.translatable(gun.getID());
                return Component.translatable(
                        "death.attack.bullet",
                        victim.getDisplayName(),
                        causingEntity.getDisplayName(),
                        gunName
                );
            }
        };
    }

    @Override
    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        shooterId = packet.getData();
        if (shooter == null) {
            Entity entity = level().getEntity(shooterId);
            if (entity instanceof LivingEntity living) {
                shooter = living;
            }
        }
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, shooterId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        shooterId = tag.getInt("shooterId");
        serverBirthTime = tag.getLong("serverBirthTime");
        markedForRemoval = tag.getBoolean("markedForRemoval");
        Vec3 velocity = new Vec3(
                tag.getDouble("vx"),
                tag.getDouble("vy"),
                tag.getDouble("vz")
        );
        setDeltaMovement(velocity);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 6400;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("shooterId", shooterId);
        Vec3 velocity = getDeltaMovement();
        tag.putDouble("vx", velocity.x);
        tag.putDouble("vy", velocity.y);
        tag.putDouble("vz", velocity.z);
        tag.putLong("serverBirthTime", serverBirthTime);
        tag.putBoolean("markedForRemoval", markedForRemoval);
    }

    public LivingEntity getShooter() {
        return shooter;
    }

    public int getShooterId() {
        return shooterId;
    }

    public void start() {
        serverBirthTime = System.currentTimeMillis();
    }
}