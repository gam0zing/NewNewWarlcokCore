package com.gam0zing.newnew_warlock_core.body;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.body.BodyPartFile.RawPart;
import com.gam0zing.newnew_warlock_core.body.hit.HitFace;
import com.gam0zing.newnew_warlock_core.body.hit.HitLocation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 自定义受击部位的运行时索引。
 * <p>数据来自 {@code data/<命名空间>/newnew_body/*.json}（见 {@link BodyPartLoader}），
 * 同一种生物的多个文件按 {@code priority} 从小到大应用，{@code over_write} 为 true 时清空已累积的部位。</p>
 *
 * <p>命中判定：按合并后的顺序遍历部位，第一个“面相同且坐标落在区域内”的部位生效；
 * 都不命中时返回 {@link BodyPart#DEFAULT}（无受击效果、伤害倍率 1）。</p>
 *
 * <p>数据里的 {@code surfaces} 是面名数组（{@code top}/{@code bottom}/{@code left}/{@code right}/{@code front}/{@code back}），
 * 解析阶段就会按数组顺序拆成多个独立部位（不写时代表全身六个面），
 * 因此合并后的列表里每个面都是单独条目，各自按顺序参与命中判定。</p>
 *
 * <p>写入部位时，若同一生物同一面上已经存在同名部位，后者会<b>整体替换</b>前者（范围、倍率、效果全部取后者），
 * 且占用前者在列表中的位置，见 {@link #upsertPart}。</p>
 *
 * <p><b>默认受击部位</b>：数据文件可以用 {@code default_part} 指定一个部位名（有且只有一个，
 * 应用顺序靠后的文件覆盖靠前的）。它在<b>拿不到受击位置</b>时使用（见 {@link #find}）：
 * 环境伤害没有可用射线、或射线连 AABB 都没命中时，先取这个部位；未配置、或该名字在合并后的部位里
 * 找不到时，才退回 {@link BodyPart#DEFAULT}。注意“命中了盒但没有部位覆盖该点”仍返回
 * {@link BodyPart#DEFAULT}，不使用默认受击部位。</p>
 *
 * <p>合并顺序：{@code priority < 0} 的数据文件 → 内置 {@code mob_id → Body} 映射（视为 priority 0，
 * 排在 priority 0 的数据文件之前）→ 其余数据文件（按 priority、文件 id 升序）。
 * 每个文件内部：先 {@code template} 引用的模板（内置 Body 或数据包模板 {@link BodyTemplates}），
 * 再本文件自定义的部位。
 * {@code over_write=true} 会清空当前已累积的全部部位（包括内置映射的部分）。</p>
 *
 * <p>身体重量与部位同源合并：内置 Body 的重量 → 引用模板的重量 → 数据文件的 {@code weight}
 * （后者覆盖前者），都没有时为 {@link Body#DEFAULT_WEIGHT}。</p>
 */
public final class BodyParts {

    /** 一次重载后的完整结果：部位表 + 各生物的默认受击部位（整体替换，读取无锁） */
    private static volatile Snapshot current = Snapshot.EMPTY;

    private BodyParts() {}

    /**
     * 按受击位置查找目标生物命中的受击部位。
     *
     * @param target   受击的生物，可为 null
     * @param location 受击位置（由 {@link com.gam0zing.newnew_warlock_core.body.hit.HitLocationResolver} 得到），可为 null
     * @return 命中的部位；位置为 null 时返回该生物的默认受击部位（未配置则 {@link BodyPart#DEFAULT}）；
     *         命中但没有部位覆盖该点时返回 {@link BodyPart#DEFAULT}
     */
    public static BodyPart find(@Nullable LivingEntity target, @Nullable HitLocation location) {
        if (target == null) {
            return BodyPart.DEFAULT;
        }
        Identifier mobId = EntityType.getKey(target.getType());
        return location == null
            ? defaultPart(mobId)
            : find(mobId, location.face(), location.x(), location.y());
    }

    /**
     * 按生物类型与受击位置查找受击部位。
     *
     * @param mobId 生物注册名
     * @param face  受击面
     * @param x     面内横坐标，0-1
     * @param y     面内纵坐标，0-1
     * @return 命中的部位；未命中时返回 {@link BodyPart#DEFAULT}
     */
    public static BodyPart find(Identifier mobId, HitFace face, float x, float y) {
        for (BodyPart part : parts(mobId)) {
            if (part.matches(face, x, y)) {
                return part;
            }
        }
        return BodyPart.DEFAULT;
    }

    /**
     * 按生物类型与受击位置查找受击部位。
     *
     * @param type 生物类型，可为 null
     * @return 命中的部位；未命中或类型为空时返回 {@link BodyPart#DEFAULT}
     */
    public static BodyPart find(@Nullable EntityType<?> type, HitFace face, float x, float y) {
        return type == null ? BodyPart.DEFAULT : find(EntityType.getKey(type), face, x, y);
    }

    /**
     * {@return 该生物的默认受击部位}（数据文件里的 {@code default_part}）。
     * <p>未配置、或配置的名字在合并后的部位里不存在时返回 {@link BodyPart#DEFAULT}。</p>
     */
    public static BodyPart defaultPart(Identifier mobId) {
        return current.defaults().getOrDefault(mobId, BodyPart.DEFAULT);
    }

    /**
     * {@return 该生物类型的默认受击部位}
     *
     * @param type 生物类型，可为 null
     */
    public static BodyPart defaultPart(@Nullable EntityType<?> type) {
        return type == null ? BodyPart.DEFAULT : defaultPart(EntityType.getKey(type));
    }

    /** {@return 某生物的合并后部位列表（不可变，按应用顺序排列）} */
    public static List<BodyPart> parts(Identifier mobId) {
        return current.layouts().getOrDefault(mobId, List.of());
    }

    /** {@return 当前已合并的部位表（只读快照，调试用）} */
    public static Map<Identifier, List<BodyPart>> snapshot() {
        return current.layouts();
    }

    /** {@return 当前各生物的默认受击部位（只读快照，调试用；未配置的生物不在其中）} */
    public static Map<Identifier, BodyPart> defaultsSnapshot() {
        return current.defaults();
    }

    /**
     * {@return 该生物的身体重量}
     * <p>数据文件与模板都没有声明时返回 {@link Body#DEFAULT_WEIGHT}。</p>
     *
     * @param mobId 生物注册名
     */
    public static double weight(Identifier mobId) {
        return current.weights().getOrDefault(mobId, Body.DEFAULT_WEIGHT);
    }

    /**
     * {@return 该生物的身体重量}
     *
     * @param target 生物，可为 null；为 null 时返回 {@link Body#DEFAULT_WEIGHT}
     */
    public static double weight(@Nullable LivingEntity target) {
        return target == null ? Body.DEFAULT_WEIGHT : weight(EntityType.getKey(target.getType()));
    }

    /**
     * 用新解析的数据整体替换索引（由 {@link BodyPartLoader} 在数据重载完成时调用）。
     * <p>合并规则：同一种生物先应用 {@code priority < 0} 的文件，再应用内置映射（视为 {@link Bodies#PRIORITY}），
     * 最后应用其余文件（同优先级按文件 id 升序，保证结果稳定）。
     * 每个文件内部顺序为：{@code over_write} 清空 → {@code template} 引用的模板
     * （内置 Body 或数据包模板，见 {@link BodyTemplates}）→ 本文件自定义部位。</p>
     * <p>写入时若列表里已有“同面 + 同名”的部位，则整体替换（见 {@link #upsertPart}）。</p>
     * <p>默认受击部位与身体重量都按应用顺序取最后一个声明（后者覆盖前者）；默认部位在部位合并完成后
     * 按名字解析，名字在合并结果里找不到时报告并退回 {@link BodyPart#DEFAULT}。</p>
     *
     * @param files 文件 id → 解析后的数据
     */
    static void replaceWith(Map<Identifier, BodyPartFile> files) {
        Map<Identifier, List<SortedFile>> byMob = new HashMap<>();

        files.entrySet().stream()
            .map(entry -> new SortedFile(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparingInt((SortedFile file) -> file.file().priority())
                .thenComparing(SortedFile::fileId))
            .forEach(file -> byMob
                .computeIfAbsent(file.file().mobId(), id -> new ArrayList<>())
                .add(file));

        // 内置 mob_id → Body 映射也算一种来源，参与合并
        for (Identifier mobId : Bodies.BY_MOB.keySet()) {
            byMob.computeIfAbsent(mobId, id -> new ArrayList<>());
        }

        Map<Identifier, List<BodyPart>> merged = new HashMap<>(byMob.size());
        Map<Identifier, BodyPart> defaults = new HashMap<>();
        Map<Identifier, Double> weights = new HashMap<>(byMob.size());
        for (Map.Entry<Identifier, List<SortedFile>> entry : byMob.entrySet()) {
            Identifier mobId = entry.getKey();
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mobId)) {
                log("mob_id=" + mobId + " 未注册，该数据不会生效（可能是对应模组未安装）");
            }

            List<BodyPart> parts = new ArrayList<>();
            boolean builtInApplied = false;
            Optional<String> declaredDefault = Optional.empty();
            double weight = Body.DEFAULT_WEIGHT;
            for (SortedFile file : entry.getValue()) {
                // 内置映射视为 PRIORITY：在第一个 priority >= PRIORITY 的文件之前应用
                if (!builtInApplied && file.file().priority() >= Bodies.PRIORITY) {
                    weight = applyBuiltIn(parts, mobId);
                    builtInApplied = true;
                }

                if (file.file().overWrite()) {
                    parts.clear();
                }

                // 先应用 template 引用的模板（内置 Body 或数据包模板），再应用本文件自定义的部位
                Optional<Body> template = file.file().templateBody(file.fileId(), BodyParts::log);
                if (template.isPresent()) {
                    Body body = template.get();
                    body.parts().forEach(part -> upsertPart(parts, part));
                    weight = body.weight();
                }

                for (RawPart raw : file.file().parts()) {
                    for (BodyPart part : raw.resolve(file.fileId(), BodyParts::log)) {
                        upsertPart(parts, part);
                    }
                }

                // 默认受击部位：按应用顺序，后声明的覆盖先声明的
                if (file.file().defaultPart().isPresent()) {
                    declaredDefault = file.file().defaultPart();
                }

                // 身体重量：按应用顺序，后声明的覆盖先声明的
                if (file.file().weight().isPresent()) {
                    weight = declaredWeight(file.file().weight().get(), file.fileId());
                }
            }
            if (!builtInApplied) {
                weight = applyBuiltIn(parts, mobId);
            }
            merged.put(mobId, List.copyOf(parts));
            weights.put(mobId, weight);
            resolveDefault(parts, declaredDefault, mobId).ifPresent(part -> defaults.put(mobId, part));
        }

        current = new Snapshot(Map.copyOf(merged), Map.copyOf(defaults), Map.copyOf(weights));
        NewNewWarlockCore.LOGGER.info(
            "[newnew_body] 已加载 {} 份数据，合并出 {} 种生物的受击部位（{} 种配置了默认受击部位）",
            files.size(), merged.size(), defaults.size());
    }

    /**
     * 应用内置 {@code mob_id → Body} 映射。
     *
     * @return 内置身体重量；没有对应内置 Body 时返回 {@link Body#DEFAULT_WEIGHT}
     */
    private static double applyBuiltIn(List<BodyPart> parts, Identifier mobId) {
        Body body = Bodies.BY_MOB.get(mobId);
        if (body == null) {
            return Body.DEFAULT_WEIGHT;
        }
        body.parts().forEach(part -> upsertPart(parts, part));
        return body.weight();
    }

    /** 读取数据文件声明的重量；非有限值或非正数时报告并沿用原值（返回 {@link Body#DEFAULT_WEIGHT} 前先告警） */
    private static double declaredWeight(double value, Identifier fileId) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            log(fileId + ": weight=" + value + " 无效（必须是正数），已忽略该重量声明");
            return Body.DEFAULT_WEIGHT;
        }
        return value;
    }

    /**
     * 写入一个部位；若列表里已有“同面 + 同名”的部位，则就地<b>整体替换</b>它
     * （范围、受伤倍率、受击效果全部取新值），并占用它在列表中的位置，
     * 因此“按顺序取第一个命中”的优先级由该名称首次出现的位置决定。
     * <p>模板内部也用它，保证 {@code data/newnew_body_template} 与 newnew_body 的语义一致。</p>
     */
    static void upsertPart(List<BodyPart> parts, BodyPart part) {
        for (int i = 0; i < parts.size(); i++) {
            BodyPart existing = parts.get(i);
            if (existing.surface() == part.surface() && existing.name().equals(part.name())) {
                parts.set(i, part);
                return;
            }
        }
        parts.add(part);
    }

    /**
     * 把 {@code default_part} 声明的名字解析成合并后的部位。
     * <p>同一个名字可能在多个面上各有一个条目（倍率与效果通常相同），取列表中第一个即可。</p>
     */
    private static Optional<BodyPart> resolveDefault(
        List<BodyPart> parts, Optional<String> declared, Identifier mobId) {
        if (declared.isEmpty()) {
            return Optional.empty();
        }
        String wanted = declared.get();
        for (BodyPart part : parts) {
            if (part.name().equals(wanted)) {
                return Optional.of(part);
            }
        }
        log(mobId + ": default_part='" + wanted + "' 在合并后的部位里不存在，该生物改用全局默认部位");
        return Optional.empty();
    }

    /** 数据警告：无效条目只跳过自身，不中断整个文件 */
    private static void log(String message) {
        NewNewWarlockCore.LOGGER.warn("[newnew_body] {}", message);
    }

    /** 排序用：文件 id + 数据 */
    private record SortedFile(Identifier fileId, BodyPartFile file) {}

    /** 一次重载后的完整只读结果 */
    private record Snapshot(
        Map<Identifier, List<BodyPart>> layouts,
        Map<Identifier, BodyPart> defaults,
        Map<Identifier, Double> weights) {

        static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of(), Map.of());
    }
}
