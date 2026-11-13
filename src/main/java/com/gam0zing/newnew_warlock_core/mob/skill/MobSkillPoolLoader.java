package com.gam0zing.newnew_warlock_core.mob.skill;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * 生物技能池绑定的数据加载器。
 * <p>扫描 {@code data/<命名空间>/mob_skill_pool/*.json}，每个文件为一位生物声明技能池的覆盖或追加，
 * 见 {@link MobSkillPoolFile}。加载结果在 {@link #apply} 里整体替换 {@link MobSkillPools} 的绑定表，
 * 因此在世界首次载入与 {@code /reload} 后都会生效。</p>
 */
public final class MobSkillPoolLoader
    extends SimplePreparableReloadListener<Map<Identifier, MobSkillPoolFile>> {

    /** 技能池绑定的数据目录名（不含 {@code data/} 与其后的层级） */
    public static final String DIRECTORY = "mob_skill_pool";

    private static final Gson GSON = new Gson();

    private MobSkillPoolLoader() {}

    /** 注册入口：挂到 NeoForge 游戏总线（{@link AddServerReloadListenersEvent}）。 */
    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, DIRECTORY),
            new MobSkillPoolLoader());
    }

    @Override
    protected Map<Identifier, MobSkillPoolFile> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        FileToIdConverter converter = FileToIdConverter.json(DIRECTORY);
        Map<Identifier, MobSkillPoolFile> parsed = new HashMap<>();
        converter.listMatchingResources(resourceManager).forEach((fileId, resource) -> {
            try (BufferedReader reader = resource.openAsReader()) {
                JsonElement json = GSON.fromJson(reader, JsonElement.class);
                parsed.put(converter.fileToId(fileId),
                    MobSkillPoolFile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            } catch (Exception exception) {
                NewNewWarlockCore.LOGGER.error("[mob_skill_pool] 无法读取或解析 {}，已跳过该文件", fileId, exception);
            }
        });
        return parsed;
    }

    @Override
    protected void apply(Map<Identifier, MobSkillPoolFile> data, ResourceManager resourceManager,
        ProfilerFiller profiler) {
        MobSkillPools.replaceWith(data);
    }
}
