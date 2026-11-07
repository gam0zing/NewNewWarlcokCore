package com.gam0zing.newnew_warlock_core.skill.tree;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * 技能树数据包加载器：{@code data/<namespace>/skill_trees/<id>.json}。
 * <p>单棵树解析失败只跳过该树并记录错误，不影响其他树；整体替换快照，读侧无锁。</p>
 */
public final class SkillTreeLoader extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {

    private static final Gson GSON = new Gson();

    private SkillTreeLoader() {}

    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "skill_trees"), new SkillTreeLoader());
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        FileToIdConverter converter = FileToIdConverter.json(SkillTreeParser.DIRECTORY);
        Map<Identifier, JsonElement> raw = new HashMap<>();
        converter.listMatchingResources(resourceManager).forEach((fileId, resource) -> {
            try (BufferedReader reader = resource.openAsReader()) {
                raw.put(converter.fileToId(fileId), GSON.fromJson(reader, JsonElement.class));
            } catch (Exception exception) {
                NewNewWarlockCore.LOGGER.error("[skill] 无法读取 {}，已跳过该文件", fileId, exception);
            }
        });
        return raw;
    }

    @Override
    protected void apply(
        Map<Identifier, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, SkillTreeDefinition> loaded = new LinkedHashMap<>();
        resources.forEach((id, element) -> {
            if (!element.isJsonObject()) {
                NewNewWarlockCore.LOGGER.error("[skill] 技能树 {} 不是 JSON 对象，已跳过", id);
                return;
            }
            JsonObject json = element.getAsJsonObject();
            try {
                loaded.put(id, SkillTreeParser.parse(id, json));
            } catch (RuntimeException exception) {
                NewNewWarlockCore.LOGGER.error("[skill] 技能树 {} 解析失败，已跳过：{}", id, exception.getMessage(), exception);
            }
        });
        SkillTrees.replaceWith(loaded);
        NewNewWarlockCore.LOGGER.info("[skill] 已加载 {} 棵技能树", loaded.size());
    }
}
