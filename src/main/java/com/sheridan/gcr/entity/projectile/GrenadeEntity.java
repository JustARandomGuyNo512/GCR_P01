package com.sheridan.gcr.entity.projectile;

import com.sheridan.gcr.client.render.fx.particles.ModParticles;
import com.sheridan.gcr.client.render.fx.particles.ember.EmberOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.FlashOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.FragmentOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.SparkOption;
import com.sheridan.gcr.damageTypes.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.function.Predicate;

public class GrenadeEntity extends Entity{
    Predicate<Entity> GENERIC_TARGETS = (input) -> input instanceof LivingEntity && !input.isSpectator() && input.isAlive();
    public LivingEntity shooter;
    int bounced = 0;
    float explodeRadius;

    public GrenadeEntity(EntityType<? extends Entity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {

    }

    public void shoot(double pX, double pY, double pZ, float pVelocity, float pInaccuracy, float explodeRadius) {
        Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().add(
                this.random.triangle(0.0D, 0.0172275D * (double)pInaccuracy),
                this.random.triangle(0.0D, 0.0172275D * (double)pInaccuracy),
                this.random.triangle(0.0D, 0.0172275D * (double)pInaccuracy)).scale(pVelocity);
        this.setDeltaMovement(vec3);
        double d0 = vec3.horizontalDistance();
        this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * 57.29577951308233));
        this.setXRot((float)(Mth.atan2(vec3.y, d0) * 57.29577951308233));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.explodeRadius = explodeRadius;
    }

    public void shootFromRotation(LivingEntity pShooter, float pX, float pY, float pZ, float pVelocity, float pInaccuracy, float explodeRadius) {
        float f = -Mth.sin(pY * 0.0174532925199433f) * Mth.cos(pX * 0.0174532925199433f);
        float f1 = -Mth.sin((pX + pZ) * 0.0174532925199433f);
        float f2 = Mth.cos(pY * 0.0174532925199433f) * Mth.cos(pX * 0.0174532925199433f);
        this.shoot(f, f1, f2, pVelocity, pInaccuracy, explodeRadius);
        this.shooter = pShooter;
        Vec3 shooterPos = new Vec3(shooter.getX(), shooter.getY() + shooter.getEyeHeight(shooter.getPose()), shooter.getZ());
        this.setPos(shooterPos.x, shooterPos.y, shooterPos.z);
    }

    private float getHitDeg(Vec3 velocity, Direction direction) {
        switch (direction) {
            case UP, DOWN -> {
                float length = (float) velocity.length();
                float cosTheta = (float) (Math.abs(velocity.y) / length);
                return  (float) Math.toDegrees(Math.acos(cosTheta));
            }
            case NORTH, SOUTH -> {
                float length = (float) velocity.length();
                float cosTheta = (float) (Math.abs(velocity.z) / length);
                return  (float) Math.toDegrees(Math.acos(cosTheta));
            }
            case WEST, EAST -> {
                float length = (float) velocity.length();
                float cosTheta = (float) (Math.abs(velocity.x) / length);
                return  (float) Math.toDegrees(Math.acos(cosTheta));
            }
        }
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount >= 220) {
            explode(Direction.UP, new Vec3(getX(), getY(), getZ()));
        }
        Vec3 deltaMovement = this.getDeltaMovement();

        if (this.level().isClientSide && this.tickCount > 4) {
            SimpleParticleType type = this.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.CRIT;
            for(int i = 0; i < 4; ++i) {
                this.level().addParticle(type,
                        this.getX() + deltaMovement.x * (double)i / 4.0D,
                        this.getY() + deltaMovement.y * (double)i / 4.0D,
                        this.getZ() + deltaMovement.z * (double)i / 4.0D,
                        -deltaMovement.x, -deltaMovement.y + 0.2D, -deltaMovement.z);
            }
        }

        Vec3 prevPos = this.position();
        Vec3 nextPos = prevPos.add(deltaMovement);
        BlockHitResult hitResult = this.level().clip(new ClipContext(prevPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hitResult.getType() != HitResult.Type.MISS) {
            if (!this.level().isClientSide) {
                Vec3 hitPos = hitResult.getLocation();
                EntityHitResult entityhitresult = this.findHitEntity(prevPos, hitPos);
                if (entityhitresult != null && entityhitresult.getEntity() != this.shooter) {
                    onHitEntity(entityhitresult.getEntity(), entityhitresult.getLocation());
                    return;
                }
            }
            if (bounced < 3) {
                if(getHitDeg(deltaMovement, hitResult.getDirection()) < 80) {
                    explode(hitResult.getDirection(), hitResult.getLocation());
                    return;
                }
                switch (hitResult.getDirection()) {
                    case UP, DOWN -> deltaMovement = new Vec3(deltaMovement.x, -deltaMovement.y, deltaMovement.z).scale(0.5f);
                    case NORTH, SOUTH -> deltaMovement = new Vec3(deltaMovement.x, deltaMovement.y, -deltaMovement.z).scale(0.5f);
                    case WEST, EAST -> deltaMovement = new Vec3(-deltaMovement.x, deltaMovement.y, deltaMovement.z).scale(0.5f);
                }
                this.level().playSound(this, hitResult.getBlockPos(), SoundEvents.IRON_GOLEM_HURT, SoundSource.BLOCKS, 1, 1);
                nextPos = hitResult.getLocation();
                bounced ++;
            } else {
                explode(hitResult.getDirection(), hitResult.getLocation());
            }
        } else {
            if (!this.level().isClientSide) {
                EntityHitResult entityhitresult = this.findHitEntity(prevPos, nextPos);
                if (entityhitresult != null && entityhitresult.getEntity() != this.shooter) {
                    onHitEntity(entityhitresult.getEntity(), entityhitresult.getLocation());
                    return;
                }
            }
        }

        this.setPos(nextPos.x, nextPos.y, nextPos.z);
        float f = this.isInWater() ? 0.88f : 0.99f;
        this.setDeltaMovement(deltaMovement.add(0, -0.04f, 0).scale(f));
        if (bounced >= 3 && this.getDeltaMovement().length() <= 0.1f) {
            explode(Direction.UP, new Vec3(getX(), getY(), getZ()));
            return;
        }
        double horizontalDistance = deltaMovement.horizontalDistance();
        this.setYRot((float)(Mth.atan2(deltaMovement.x, deltaMovement.z) * (double)(180F / (float)Math.PI)));
        this.setXRot((float)(Mth.atan2(deltaMovement.y, horizontalDistance) * (double)(180F / (float)Math.PI)));
        this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dis) {
        return dis <= 32768;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.tickCount = pCompound.getInt("tick");
        this.bounced = pCompound.getShort("bounced");
        this.explodeRadius = pCompound.getFloat("radius");
    }

    public void explode(Direction hitDir, Vec3 location) {
        if (!this.level().isClientSide) {
            //this.level().explode(this, Explosion.getDefaultDamageSource(this.level(), this), null,location.x, location.y, location.z, explodeRadius, false, Level.ExplosionInteraction.NONE, false,  ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SoundEvents.GENERIC_EXPLODE);
            customExplode(location, 40, 10, 6);
            //TODO:增加自定义炫酷爆炸效果
            spawnCustomExplosionEffect((ServerLevel) this.level(), location.x, location.y, location.z, hitDir);

            this.discard();

        }
    }

    public void customExplode(Vec3 location, float centerDamage, float edgeDamage, double radius) {
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();

        AABB aabb = new AABB(
                location.x - radius, location.y - radius, location.z - radius,
                location.x + radius, location.y + radius, location.z + radius
        );

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aabb);
        Entity causing = this.shooter == null ? this : this.shooter;
        DamageSource source = getCustomExplosionSource(serverLevel, causing);
        for (LivingEntity target : targets) {
            double distance = target.position().distanceTo(location);
            if (distance > radius) {
                continue;
            }


            double exposure = getSeenPercent(location, target);
            if (exposure <= 0) {
                continue;
            }

            double falloff = 1.0 - (distance / radius);
            float damage = (float) (edgeDamage + (centerDamage - edgeDamage) * falloff);
            damage *= (float) exposure;

            if (damage > 0) {
                target.hurt(source, damage);
//                if (this.shooter instanceof ServerPlayer serverPlayer) {
//                    if (target instanceof ServerPlayer targetPlayer) {
//                        System.out.println(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) + " " +
//                                serverPlayer.canHarmPlayer(targetPlayer));
//                        float amount = Math.max(0.0F, source.type().scaling().getScalingFunction().scaleDamage(source, targetPlayer, 10, this.level().getDifficulty()));
//                        System.out.println("scaled amount: " + amount);
//                    }
//                }

                Vec3 knockDir = target.position().subtract(location).normalize();
                double knockStrength = 0.5 * falloff + 0.1;
                target.setDeltaMovement(target.getDeltaMovement().add(
                        knockDir.x * knockStrength,
                        knockDir.y * knockStrength + 0.1,
                        knockDir.z * knockStrength
                ));
                target.hurtMarked = true;
            }
        }

    }

    private DamageSource getCustomExplosionSource(ServerLevel level, Entity causing) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ModDamageTypes.CUSTOM_EXPLOSION);
        return new DamageSource(holder, this, causing);
    }

    private double getSeenPercent(Vec3 center, Entity target) {
        AABB box = target.getBoundingBox();
        double xStep = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double yStep = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double zStep = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        if (xStep < 0 || yStep < 0 || zStep < 0) {
            return 0;
        }

        int hit = 0, total = 0;
        for (double x = 0; x <= 1; x += xStep) {
            for (double y = 0; y <= 1; y += yStep) {
                for (double z = 0; z <= 1; z += zStep) {
                    Vec3 sample = new Vec3(
                            Mth.lerp(x, box.minX, box.maxX),
                            Mth.lerp(y, box.minY, box.maxY),
                            Mth.lerp(z, box.minZ, box.maxZ)
                    );
                    if (target.level().clip(new ClipContext(center, sample,
                                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target))
                            .getType() == HitResult.Type.MISS) {
                        hit++;
                    }
                    total++;
                }
            }
        }
        return total == 0 ? 0 : (double) hit / total;
    }

    static int rgb = new Color(255, 250, 200).getRGB();
    static int rgb2 = new Color(255, 75, 25).getRGB();
    private void spawnCustomExplosionEffect(ServerLevel level, double x, double y, double z, Direction hitDir) {
        if (level.players().isEmpty()) {
            return;
        }
        List<ServerPlayer> players = level.players();
        FragmentOption fragmentOptions = new FragmentOption(
                explodeRadius,
                (int) (explodeRadius * 50),
                6f,
                rgb
        );

        SparkOption sparkOptions = new SparkOption(
                explodeRadius * 0.5f,
                (int) (explodeRadius * 10),
                4f,
                rgb,
                rgb2
        );

        int heatSmokeCount = (int) Mth.clamp(explodeRadius, 1, 10);
        float smokeScale = Mth.clamp(explodeRadius, 1, 5);
        EmberOption emberOptions = new EmberOption(
                ModParticles.HEAT_SMOKE.get(),
                8,
                EmberOption.EasingType.SQR,
                15,
                smokeScale,
                true
        );
        for (int i = 0; i < heatSmokeCount; i ++) {
            float randomX = (level().random.nextFloat() - 0.5f) * explodeRadius;
            float randomY = (level().random.nextFloat() - 0.2f) * explodeRadius * 0.75f;
            float randomZ = (level().random.nextFloat() - 0.5f) * explodeRadius;
            emberOptions.addEntry(randomX, randomY, randomZ);
        }
        FlashOption flashOptions = new FlashOption(
                explodeRadius * (0.5f + level.random.nextFloat() * 0.1f)
        );
        ClientboundLevelParticlesPacket fragmentPacket = null;
        ClientboundLevelParticlesPacket sparkPacket = null;
        ClientboundLevelParticlesPacket emberPacket = null;
        ClientboundLevelParticlesPacket flashPacket = null;
        if (players.size() > 1 || players.getFirst() != this.shooter) {
            fragmentPacket = new ClientboundLevelParticlesPacket(fragmentOptions, false, x, y, z, 0, 0, 0, 0, 1);
            sparkPacket = new ClientboundLevelParticlesPacket(sparkOptions, false, x, y, z, 0, 0, 0, 0, 1);
            emberPacket = new ClientboundLevelParticlesPacket(emberOptions, false, x, y, z, 0, 0, 0, 0, 1);
            flashPacket = new ClientboundLevelParticlesPacket(flashOptions, false, x, y, z, 0, 0, 0, 0, 1);
        }
        for (ServerPlayer serverPlayer : players) {
            if (serverPlayer == this.shooter) {
                level.sendParticles(serverPlayer, fragmentOptions, true, x, y, z, 1, 0, 0, 0, 0.0);
                level.sendParticles(serverPlayer, sparkOptions, true, x, y, z, 1, 0, 0, 0, 0.0);
                level.sendParticles(serverPlayer, emberOptions, true, x, y, z, 1, 0, 0, 0, 0.0);
                level.sendParticles(serverPlayer, flashOptions, true, x, y, z, 1, 0, 0, 0, 0.0);
            } else {
                BlockPos blockpos = serverPlayer.blockPosition();
                if (blockpos.closerToCenterThan(new Vec3(x, y, z), 64.0F)) {
                    assert fragmentPacket != null;
                    serverPlayer.connection.send(fragmentPacket);
                    serverPlayer.connection.send(sparkPacket);
                    serverPlayer.connection.send(emberPacket);
                    serverPlayer.connection.send(flashPacket);
                }
            }
        }
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 4f, 0.9f);
    }

    private void onHitEntity(Entity entity, Vec3 location) {
        explode(Direction.UP, location);
        entity.invulnerableTime = 0;
        float length = (float) this.getDeltaMovement().length();
        if (length > 0.5f) {
            DamageSource damageSource = this.shooter == null ?
                    damageSources().generic() :
                    damageSources().mobProjectile(this, this.shooter);
            entity.hurt(damageSource, length * 2f);
        }
    }


    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putInt("tick", tickCount);
        pCompound.putShort("bounced", (short) bounced);
        pCompound.putFloat("radius", explodeRadius);
    }

    protected EntityHitResult findHitEntity(Vec3 pStartVec, Vec3 pEndVec) {
        return ProjectileUtil.getEntityHitResult(this.level(), this, pStartVec, pEndVec, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D), GENERIC_TARGETS);
    }

    protected static float lerpRotation(float pCurrentRotation, float pTargetRotation) {
        while(pTargetRotation - pCurrentRotation < -180.0F) {
            pCurrentRotation -= 360.0F;
        }
        while(pTargetRotation - pCurrentRotation >= 180.0F) {
            pCurrentRotation += 360.0F;
        }
        return Mth.lerp(0.2F, pCurrentRotation, pTargetRotation);
    }

    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return new ClientboundAddEntityPacket(
                this.getId(), this.getUUID(), this.getX(), this.getY(), this.getZ(), this.getXRot(),
                this.getYRot(), this.getType(), 0, this.getDeltaMovement(), this.getYHeadRot()
        );
    }

    public void recreateFromPacket(@NotNull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.blocksBuilding = true;
        double d0 = packet.getX();
        double d1 = packet.getY();
        double d2 = packet.getZ();
        this.setPos(d0, d1, d2);
        this.noCulling = true;
    }
}


