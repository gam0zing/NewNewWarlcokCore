package com.gam0zing.newnew_warlock_core.body;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * 自定义受击部位的数据加载器。
 * <p>一次重载同时扫描两个目录：</p>
 * <ul>
 *     <li>{@code data/<命名空间>/newnew_body/*.json} —— 每种生物的受击部位，见 {@link BodyPartFile}；</li>
 *     <li>{@code data/<命名空间>/newnew_body_template/*.json} —— 可被 newnew_body 的 {@code template}
 *     引用的身体模板，见 {@link BodyTemplateFile}（文件必须直接放在根目录，id 为“命名空间 + 文件名”）。</li>
 * </ul>
 * <p>应用顺序固定为“模板先就位、再合并生物部位”，所以同一次重载里 {@code template} 一定能引用到本次加载的模板；
 * 两个快照各自整体替换，读侧无锁。</p>
 * <p>该监听器在服务端资源重载时执行（世界首次载入、{@code /reload}、单人游戏的集成服务器），
 * 因此受击部位的判定使用服务端数据；加载完成前 {@link BodyParts} 与 {@link BodyTemplates} 为空，
 * 全部退回默认部位。</p>
 */
public final class BodyPartLoader extends SimplePreparableReloadListener<BodyPartLoader.Loaded> {

    /** 生物受击部位的数据目录名（不含 {@code data/} 与其后的层级） */
    public static final String DIRECTORY = "newnew_body";

    /** 身体模板的数据目录名 */
    public static final String TEMPLATE_DIRECTORY = "newnew_body_template";

    /** 模板里不支持的字段（模板不支持继承，也不参与布局顺序） */
    private static final List<String> UNSUPPORTED_TEMPLATE_FIELDS =
        List.of("template", "priority", "over_write", "mob_id", "default_part");

    private static final Gson GSON = new Gson();

    private BodyPartLoader() {}

    /** 注册入口：挂到 NeoForge 游戏总线（{@link AddServerReloadListenersEvent}） */
    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, DIRECTORY), new BodyPartLoader());
    }

    /** 扫描目录并读成原始 JSON；解析交给各文件的 CODEC，便于先检查废弃字段 */
    private static Map<Identifier, JsonElement> scanJson(ResourceManager resourceManager, String directory) {
        FileToIdConverter converter = FileToIdConverter.json(directory);
        Map<Identifier, JsonElement> raw = new HashMap<>();
        converter.listMatchingResources(resourceManager).forEach((fileId, resource) -> {
            try (BufferedReader reader = resource.openAsReader()) {
                raw.put(converter.fileToId(fileId), GSON.fromJson(reader, JsonElement.class));
            } catch (Exception exception) {
                NewNewWarlockCore.LOGGER.error("[{}] 无法读取 {}，已跳过该文件", directory, fileId, exception);
            }
        });
        return raw;
    }

    @Override
    protected Loaded prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return new Loaded(parseBodyFiles(resourceManager), parseTemplateFiles(resourceManager));
    }

    @Override
    protected void apply(Loaded data, ResourceManager resourceManager, ProfilerFiller profiler) {
        // 模板先就位：newnew_body 的 template 可能引用本次加载的模板
        BodyTemplates.replaceWith(data.templates());
        BodyParts.replaceWith(data.files());
    }

    // ==========================================================================================
    // newnew_body
    // ==========================================================================================

    private static Map<Identifier, BodyPartFile> parseBodyFiles(ResourceManager resourceManager) {
        Map<Identifier, JsonElement> raw = scanJson(resourceManager, DIRECTORY);

        Map<Identifier, BodyPartFile> parsed = new HashMap<>(raw.size());
        raw.forEach((fileId, json) -> {
            warnLegacyFields(fileId, json);
            try {
                parsed.put(fileId, BodyPartFile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            } catch (RuntimeException exception) {
                NewNewWarlockCore.LOGGER.error("[newnew_body] 无法解析 {}，已跳过该文件", fileId, exception);
            }
        });
        return parsed;
    }

    /**
     * 提醒已废弃的旧字段：编解码器会忽略未知字段，若不提示，旧数据会静默退回默认值。
     * <ul>
     *     <li>{@code surface_id} → 改用 {@code surfaces} 面名数组</li>
     *     <li>{@code level} → 改用 {@code amplifier}</li>
     * </ul>
     */
    private static void warnLegacyFields(Identifier fileId, JsonElement json) {
        if (!(json instanceof JsonObject root)) {
            return;
        }
        if (!(root.get("parts") instanceof JsonArray parts)) {
            return;
        }
        for (JsonElement partElement : parts) {
            if (!(partElement instanceof JsonObject part)) {
                continue;
            }
            if (part.has("surface_id")) {
                NewNewWarlockCore.LOGGER.warn(
                    "[newnew_body] {}: part '{}' 使用了已废弃的 surface_id，请改用 surfaces 面名数组",
                    fileId, part.get("part_name"));
            }
            if (!(part.get("effects") instanceof JsonArray effects)) {
                continue;
            }
            for (JsonElement effectElement : effects) {
                if (effectElement instanceof JsonObject effect && effect.has("level")) {
                    NewNewWarlockCore.LOGGER.warn(
                        "[newnew_body] {}: effect '{}' 使用了已废弃的 level，请改用 amplifier（0 表示 I 级）",
                        fileId, effect.get("effect_id"));
                }
            }
        }
    }

    // ==========================================================================================
    // newnew_body_template
    // ==========================================================================================

    /**
     * 扫描并解析模板文件。
     * <p>模板 id 为“命名空间 + 文件名”，因此只接受 <b>直接位于目录根下</b>的文件；
     * 子目录里的文件（其 id 会带 {@code /}）会被跳过并告警。</p>
     */
    private static Map<Identifier, BodyTemplateFile> parseTemplateFiles(ResourceManager resourceManager) {
        Map<Identifier, JsonElement> raw = scanJson(resourceManager, TEMPLATE_DIRECTORY);

        Map<Identifier, BodyTemplateFile> parsed = new HashMap<>(raw.size());
        raw.forEach((fileId, json) -> {
            if (fileId.getPath().contains("/")) {
                NewNewWarlockCore.LOGGER.warn(
                    "[newnew_body_template] {} 位于子目录，已跳过：模板必须直接放在 {}/<命名空间>/{} 下"
                        + "（id 取命名空间 + 文件名）",
                    fileId, "data", TEMPLATE_DIRECTORY);
                return;
            }
            if (NewNewWarlockCore.MOD_ID.equals(fileId.getNamespace())) {
                NewNewWarlockCore.LOGGER.warn(
                    "[newnew_body_template] {} 占用了内置命名空间 {}，已跳过（内置模板优先，"
                        + "以免将来新增内置模板时静默改变已有数据包的含义）",
                    fileId, NewNewWarlockCore.MOD_ID);
                return;
            }

            warnUnsupportedTemplateFields(fileId, json);
            try {
                parsed.put(fileId, BodyTemplateFile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            } catch (RuntimeException exception) {
                NewNewWarlockCore.LOGGER.error(
                    "[newnew_body_template] 无法解析 {}，已跳过该文件", fileId, exception);
            }
        });
        return parsed;
    }

    /**
     * 提醒模板里不生效的字段：模板不支持继承，每个模板都独立定义，只认 {@code parts} 与 {@code weight}。
     */
    private static void warnUnsupportedTemplateFields(Identifier fileId, JsonElement json) {
        if (!(json instanceof JsonObject root)) {
            return;
        }
        List<String> present = new ArrayList<>();
        for (String field : UNSUPPORTED_TEMPLATE_FIELDS) {
            if (root.has(field)) {
                present.add(field);
            }
        }
        if (!present.isEmpty()) {
            NewNewWarlockCore.LOGGER.warn(
                "[newnew_body_template] {}: 模板只支持 parts 与 weight，字段 {} 不会生效"
                    + "（模板独立定义，不支持继承与布局顺序）",
                fileId, present);
        }
    }

    /** 一次重载解析出的两部分数据（模板 + 生物部位），在 {@link #apply} 里一起生效 */
    record Loaded(
        Map<Identifier, BodyPartFile> files,
        Map<Identifier, BodyTemplateFile> templates) {}
}
