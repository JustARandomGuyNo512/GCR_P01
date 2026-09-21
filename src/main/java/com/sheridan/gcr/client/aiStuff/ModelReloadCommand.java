package com.sheridan.gcr.client.aiStuff;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.modularSys.ModuleRegister;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 客户端模型热重载命令：{@code /gcr:reload_model <modular_id>}。
 *
 * <p>这是热重载的唯一入口。命令注册在客户端命令派发器上（不会发往服务器），
 * 参数支持 tab 补全 {@link ModuleRegister} 里的所有模块 id，
 * 也可以只写简写（例如 {@code m4a1}，会自动补上 {@code gcr} 命名空间）。</p>
 *
 * <p>实际的重置逻辑在 {@link ClientModelHotReloader#reloadModel(String)}。</p>
 */
@EventBusSubscriber(modid = GCR.MODID, value = Dist.CLIENT)
public final class ModelReloadCommand {

    /** 命令字面量，完整形式为 {@code /gcr:reload_model <modular_id>}。 */
    public static final String COMMAND = "gcr:reload_model";

    /** 模块 id 参数名。 */
    public static final String ARG_MODULAR_ID = "modular_id";

    private ModelReloadCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!GCR.IS_DEVELOPMENT) {
            // 开发工具：正式环境不注册
            return;
        }
        event.getDispatcher().register(
                Commands.literal(COMMAND)
                        // 必须用 greedyString：string()/word() 的非引号模式不接受 ':'，
                        // 会把 gcr:m4a1 截断成 gcr 并报 "参数后应有空格分隔"
                        .then(Commands.argument(ARG_MODULAR_ID, StringArgumentType.greedyString())
                                .suggests(ModelReloadCommand::suggestModuleIds)
                                .executes(context ->
                                        ClientModelHotReloader.reloadModel(resolveModuleId(context)) ? 1 : 0))
        );
    }

    /** 允许省略命名空间：{@code m4a1} 等价于 {@code gcr:m4a1}。 */
    private static String resolveModuleId(CommandContext<CommandSourceStack> context) {
        String raw = StringArgumentType.getString(context, ARG_MODULAR_ID);
        return raw.indexOf(':') >= 0 ? raw : GCR.MODID + ":" + raw;
    }

    private static CompletableFuture<Suggestions> suggestModuleIds(CommandContext<CommandSourceStack> context,
                                                                   SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ModuleRegister.all().keySet(), builder);
    }
}
