package com.gam0zing.newnew_warlock_core.skill.command;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

/**
 * 启动时把全部已注册的命令类型导出为定义表，供网页技能树工具导入。
 * <p>输出到 {@code <游戏目录>/newnew_warlock_core/command_definitions.json}，每次启动覆盖。
 * 只包含基本类型参数，enum 等编辑期辅助信息不进入定义表。</p>
 */
public final class CommandDefinitionExporter {

    public static final int SCHEMA_VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private CommandDefinitionExporter() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CommandDefinitionExporter::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CommandDefinitionExporter::write);
    }

    private static void write() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", SCHEMA_VERSION);
        JsonArray commands = new JsonArray();
        for (PointAllocationCommandType<?> type : PointAllocationCommands.all()) {
            commands.add(toJson(type));
        }
        root.add("commands", commands);

        Path path = FMLPaths.GAMEDIR.get().resolve(NewNewWarlockCore.MOD_ID).resolve("command_definitions.json");
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            NewNewWarlockCore.LOGGER.info("[skill] 已导出 {} 个命令类型到 {}", commands.size(), path);
        } catch (IOException exception) {
            NewNewWarlockCore.LOGGER.error("[skill] 导出命令定义表失败：{}", path, exception);
        }
    }

    private static JsonObject toJson(PointAllocationCommandType<?> type) {
        JsonObject json = new JsonObject();
        json.addProperty("id", type.id().toString());
        json.addProperty("name", type.displayName());
        json.addProperty("doc", type.documentation());
        json.addProperty("summary", type.summary());

        JsonArray params = new JsonArray();
        for (ParamSpec spec : type.params()) {
            JsonObject param = new JsonObject();
            param.addProperty("name", spec.name());
            param.addProperty("type", spec.type().id());
            if (spec.defaultValue() instanceof Number number) {
                param.addProperty("default", number);
            } else if (spec.defaultValue() instanceof Boolean bool) {
                param.addProperty("default", bool);
            } else if (spec.defaultValue() != null) {
                param.addProperty("default", String.valueOf(spec.defaultValue()));
            }
            params.add(param);
        }
        json.add("params", params);
        return json;
    }
}
