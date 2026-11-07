package com.gam0zing.newnew_warlock_core.body;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.resources.Identifier;

/**
 * 数据包提供的身体模板库。
 * <p>数据来自 {@code data/<命名空间>/newnew_body_template/*.json}（见 {@link BodyTemplateFile}），
 * 模板 id 为“命名空间 + 文件名”。这些模板由 newnew_body 数据文件的
 * {@code template} 字段引用（见 {@link BodyPartFile#templateBody}）。</p>
 *
 * <p><b>内置优先</b>：同一个 id 同时存在于内置 {@link Bodies} 时永远取内置的，
 * 因此数据包不能占用模组自己的命名空间（{@code newnew_warlock_core}），
 * 否则以后新增内置模板会静默改变已有数据包的含义。</p>
 *
 * <p>写入是整体替换（{@code volatile} 快照），读无锁；重载完成前模板库为空。</p>
 */
public final class BodyTemplates {

    /** 当前重载周期内的全部数据包模板（一次性整体替换） */
    private static volatile Map<Identifier, Body> current = Map.of();

    private BodyTemplates() {}

    /**
     * 按 id 解析模板，<b>先内置、后数据包</b>。
     *
     * @param id 带命名空间的模板 id，例如 {@code newnew_warlock_core:human_like} 或 {@code foo:my_human}；可为 null
     * @return 对应的 Body；id 为空或两边都找不到时返回 {@link Optional#empty()}
     */
    public static Optional<Body> resolve(@Nullable Identifier id) {
        Optional<Body> builtIn = Bodies.byId(id);
        return builtIn.isPresent() ? builtIn : byId(id);
    }

    /**
     * 只查数据包模板（不含内置）。
     *
     * @param id 带命名空间的模板 id；可为 null
     * @return 对应的 Body；找不到时返回 {@link Optional#empty()}
     */
    public static Optional<Body> byId(@Nullable Identifier id) {
        return id == null ? Optional.empty() : Optional.ofNullable(current.get(id));
    }

    /** {@return 当前的数据包模板表（只读快照，调试用）} */
    public static Map<Identifier, Body> snapshot() {
        return current;
    }

    /**
     * 用新解析的数据整体替换模板库（由 {@link BodyPartLoader} 在数据重载完成时、合并部位之前调用）。
     *
     * @param files 模板 id → 解析后的原始数据
     */
    static void replaceWith(Map<Identifier, BodyTemplateFile> files) {
        Map<Identifier, Body> resolved = new HashMap<>(files.size());
        files.forEach((id, file) -> resolved.put(id, file.resolve(id, BodyTemplates::log)));
        current = Map.copyOf(resolved);

        NewNewWarlockCore.LOGGER.info(
            "[newnew_body_template] 已加载 {} 个模板", resolved.size());
    }

    /** 数据警告：无效条目只跳过自身，不中断整个文件 */
    private static void log(String message) {
        NewNewWarlockCore.LOGGER.warn("[newnew_body_template] {}", message);
    }
}
