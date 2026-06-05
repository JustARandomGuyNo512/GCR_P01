package com.sheridan.gcr.client.animation.command;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class CommandFactory {
    public static final CommandFactory INSTANCE = new CommandFactory();

    private final Map<String, BiFunction<String, Float, Command>> COMMAND_MAP = new HashMap<>();

    public void registerCommandProvider(String command, BiFunction<String, Float, Command> provider) {
        if (isCommandRegistered(command)) {
            return;
        }
        COMMAND_MAP.put(command, provider);
    }

    public boolean isCommandRegistered(String command) {
        return COMMAND_MAP.containsKey(command);
    }

    @Nullable
    public Command createCommand(String rawData, float timeStamp) {
        int i = rawData.indexOf("(");
        String commandName = i > 0 ? rawData.substring(0, i) : rawData.replace(";", "");
        BiFunction<String, Float, Command> provider = COMMAND_MAP.get(commandName);
        if (provider != null) {
            return provider.apply(rawData, timeStamp);
        }
        return null;
    }

    static {
        INSTANCE.registerCommandProvider("c_left_arm", ArmPoseLerp::new);
        INSTANCE.registerCommandProvider("c_right_arm", ArmPoseLerp::new);
        INSTANCE.registerCommandProvider("mask_mag_ammo", MaskMagAmmo::new);
        INSTANCE.registerCommandProvider("ads_pose_limit", AdsPoseLimit::new);
        INSTANCE.registerCommandProvider("exit_ads", ExitAds::new);
        INSTANCE.registerCommandProvider("show_msg", ShowMsgCommand::new);
    }
}
