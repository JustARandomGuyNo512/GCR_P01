package com.sheridan.gcr.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
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
    private int life;
    private LivingEntity shooter;
    public static final EntityDataAccessor<Vector3f> EXACT_VELOCITY =
            SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.VECTOR3);
    private int shooterId;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public void setShooter(LivingEntity shooter) {
        this.shooter = shooter;
        this.shooterId = shooter.getId();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(EXACT_VELOCITY, new Vector3f(0.0F, 0.0F, 0.0F));
    }

    @Override
    public void setDeltaMovement(@NotNull Vec3 velocity) {
        super.setDeltaMovement(velocity);
        if (!this.level().isClientSide) {
            this.getEntityData().set(EXACT_VELOCITY, velocity.toVector3f());
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
        if (System.currentTimeMillis() - serverBirthTime > 2500) {
            discard();
            return;
        }
        Vec3 start = position();
        Vec3 velocity = getDeltaMovement();
        Vec3 end = start.add(velocity);

        // ✨ 修改：自定义 ClipContext，忽略树叶方块
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
            entityCheckEnd = blockHit.getLocation(); // 实体检测只到方块
        }

        // 实体碰撞（限制到非树叶方块之前）
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level(),
                this,
                start,
                entityCheckEnd,
                getBoundingBox().expandTowards(velocity).inflate(1.0),
                e -> e != this && e != this.shooter && e.isPickable()
        );

        if (entityHit != null) {
            onHitEntity(entityHit);
            return;
        }

        // 如果实体没命中，再处理非树叶 block
        if (blockHit.getType() != HitResult.Type.MISS) {
            onHitBlock(blockHit);
            return;
        }

        // 移动
        setPos(end);
    }

    private void onHitBlock(BlockHitResult hit) {
        Level world = this.level();

        if (!world.isClientSide && world instanceof ServerLevel serverWorld) {
            BlockPos pos = hit.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Direction direction = hit.getDirection();
            Block block = state.getBlock();

            if (interactWith(pos, state, direction, block, hit)) {
                // 1. 处理音效
                SoundType soundType = state.getSoundType();
                // 判断是否为金属：通过音效类型是否为金属（METAL/NETHERITE_BLOCK）或者通过材质标签判断
                boolean isMetal = soundType == SoundType.METAL || soundType == SoundType.NETHERITE_BLOCK;

                if (isMetal) {
                    world.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.8F);
                } else {
                    world.playSound(null, pos, soundType.getBreakSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
                }

                // 2. 处理粒子效果（生成垂直于面的速度）
                Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
                // 粒子初速度：沿法线方向飞出
                double speed = 0.15;
                double vx = normal.x * speed;
                double vy = normal.y * speed;
                double vz = normal.z * speed;

                // 创建对应方块的碎片粒子数据
                BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, state);
                Vec3 hitLocation = hit.getLocation();

                // 循环生成若干个带速度的方向粒子
                for (int i = 0; i < 8; i++) {
                    // 添加轻微的随机散射，让粒子效果更自然
                    double rx = vx + (world.random.nextDouble() - 0.5) * 0.05;
                    double ry = vy + (world.random.nextDouble() - 0.5) * 0.05;
                    double rz = vz + (world.random.nextDouble() - 0.5) * 0.05;

                    serverWorld.sendParticles(
                            particleData,
                            hitLocation.x, hitLocation.y, hitLocation.z,
                            0, // 数量设为0，以便让后面的 speed 参数代表速度矢量的坐标
                            rx, ry, rz,
                            1.0 // 速度缩放系数
                    );
                }

//                // 3. 新增：添加自定义弹孔粒子（如果不是透明方块）
//                if (!(block instanceof TransparentBlock)) {
//                    // 建议将方向信息通过速度参数（dx, dy, dz）传给客户端，用来旋转弹孔贴图
//                    // 数量设为 0 时，rx, ry, rz 会直接作为参数传递给客户端粒子的构造器
//                    serverWorld.sendParticles(
//                            ModParticles.BULLET_HOLE.get(), // 替换为你实际的粒子注册 DeferredHolder
//                            hitLocation.x, hitLocation.y, hitLocation.z,
//                            0,
//                            direction.get3DDataValue(), 0, 0, // 把平面的朝向索引存在第一个参数里
//                            1.0
//                    );
//                }
            }
        }
        discard();

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


    private void onHitEntity(EntityHitResult hit) {
        Entity target = hit.getEntity();
        target.invulnerableTime = 0;
        target.hurt(
                damageSources().generic(),
                6f
        );
        discard();
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
        return new ClientboundAddEntityPacket(this, entity, shooterId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        life = tag.getInt("life");
        shooterId = tag.getInt("shooterId");
        serverBirthTime = tag.getLong("serverBirthTime");
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
        tag.putInt("life", life);
        tag.putInt("shooterId", shooterId);
        Vec3 velocity = getDeltaMovement();
        tag.putDouble("vx", velocity.x);
        tag.putDouble("vy", velocity.y);
        tag.putDouble("vz", velocity.z);
        tag.putLong("serverBirthTime", serverBirthTime);
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