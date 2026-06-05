package com.sheridan.gcr.client.animation;

import com.google.common.collect.Maps;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.animation.command.Command;
import com.sheridan.gcr.network.c2s.PlaySoundPacket;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.compress.utils.Lists;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class AnimationDef {

    private final float lengthInSeconds;
    private final boolean looping;
    private final boolean keepOnLastFrame;
    private final Map<String, List<AnimationChannel>> boneAnimations;
    private final List<SoundPoint> soundPoints;
    private final List<Command> commands;

    public AnimationDef(
            float lengthInSeconds, boolean looping, boolean stopAtLastFrame,
            Map<String, List<AnimationChannel>> boneAnimations, List<SoundPoint> soundPoints, List<Command> commandPoints) {
        this.lengthInSeconds = lengthInSeconds;
        this.looping = looping;
        this.boneAnimations = boneAnimations;
        this.soundPoints = soundPoints;
        this.commands = commandPoints;
        this.keepOnLastFrame = stopAtLastFrame;
        for (Command command : commands) {
            command.bindDef(this);
        }
    }
    public float lengthInSeconds() {
        return this.lengthInSeconds;
    }

    public boolean looping() {
        return this.looping;
    }

    public Map<String, List<AnimationChannel>> boneAnimations() {
        return this.boneAnimations;
    }

    public Set<String> allBones() {
        return this.boneAnimations.keySet();
    }

    public List<SoundPoint> getSoundPoints() {
        return this.soundPoints;
    }

    public List<Command> getCommands() {return this.commands;}

    public boolean keepOnLastFrame() {
        return this.keepOnLastFrame;
    }

    public AnimationInstance asInstance() {
        return this.asInstance(true, true, new Vector3f(0.0625f));
    }

    public AnimationInstance asInstance(boolean enableSound, boolean soundOnServer, Vector3f scales) {
        return new AnimationInstance(this, scales)
                .enableSound(enableSound)
                .soundOnServer(soundOnServer);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final float length;
        private final Map<String, List<AnimationChannel>> animationByBone = Maps.newHashMap();
        private boolean looping;
        private List<SoundPoint> soundPoints;
        private final List<Command> commandPoints = new ArrayList<>();
        private boolean stopAtLastFrame;

        public static Builder withLength(float pLengthInSeconds) {
            return new Builder(pLengthInSeconds);
        }

        private Builder(float pLengthInSeconds) {
            this.length = pLengthInSeconds;
        }

        public Builder setStopAtLastFrame(boolean stopAtLastFrame) {
            this.stopAtLastFrame = stopAtLastFrame;
            return this;
        }

        public Builder setLooping(boolean looping) {
            this.looping = looping;
            return this;
        }

        public Builder addAnimation(String pBone, AnimationChannel pAnimationChannel) {
            (this.animationByBone.computeIfAbsent(pBone, (p_232278_) -> Lists.newArrayList())).add(pAnimationChannel);
            return this;
        }

        public Builder addSoundPoint(SoundPoint soundPoint) {
            if (soundPoints == null) {
                soundPoints = new ArrayList<>();
                soundPoints.add(new SoundPoint(0, SoundPoint.EMPTY_SOUND));
            }
            soundPoints.add(soundPoint);
            return this;
        }

        public Builder addCommandPoint(Command commandPoint) {
            commandPoints.add(commandPoint);
            return this;
        }

        public AnimationDef build() {
            return new AnimationDef(
                    this.length, this.looping, this.stopAtLastFrame, this.animationByBone, soundPoints, commandPoints);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SoundPoint{
        public static final ResourceLocation EMPTY_SOUND = GCR.RL(GCR.MODID, "empty_sound");
        public int tick;
        public ResourceLocation soundName;

        public SoundPoint(int tick, ResourceLocation soundName) {
            this.tick = tick;
            this.soundName = soundName;
        }

        public void playSound(boolean soundOnServer) {
            if (EMPTY_SOUND.equals(this.soundName)) {
                return;
            }
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                ModSounds.sound(1,1, player, soundName);
                if (soundOnServer) {

                    PacketDistributor.sendToServer(new PlaySoundPacket(soundName.toString(), 1, 1, player.getX(), player.getY(), player.getZ()));
                }
            }
        }
    }
}
