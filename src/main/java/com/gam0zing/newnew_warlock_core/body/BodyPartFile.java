package com.gam0zing.newnew_warlock_core.body;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.gam0zing.newnew_warlock_core.body.hit.HitFace;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * 一份 newnew_body 数据文件（{@code data/<命名空间>/newnew_body/<文件名>.json}），原样反映 JSON 内容。
 * <p>同一种生物的多个文件按 {@link #priority()} 从小到大应用，{@link #overWrite()} 为 true 时
 * 会先清空已经累积的部位再添加自己的部位，否则直接追加。</p>
 * <p>{@link #template()} 可引用一个身体模板（必须写完整命名空间）：既可以是内置 Body
 * （例如 {@code newnew_warlock_core:human_like}），也可以是数据包提供的模板
 * （{@code data/<命名空间>/newnew_body_template/<文件名>.json}，见 {@link BodyTemplateFile}），
 * 其部位在本文件自定义的部位之前应用。解析时内置模板优先，见 {@link BodyTemplates}。</p>
 * <p>{@link #defaultPart()} 指定该生物的“默认受击部位”（一个已存在的部位名），
 * 用于无法解析受击位置、或用朝向盒重投影仍然落空的伤害。</p>
 *
 * <p>{@link #weight()} 是该生物的身体重量，缺省时沿用模板/内置身体的重量，最终缺省 1。</p>
 *
 * @param overWrite  是否覆盖此前已累积的部位（而不是追加）
 * @param priority   应用顺序，值小的先应用（缺省 0）
 * @param mobId      生物的注册名，例如 {@code minecraft:zombie}
 * @param template   身体模板的引用（缺省不引用）
 * @param parts      受击部位数组（缺省为空）
 * @param defaultPart 默认受击部位的名字（缺省没有）；有且只有一个，多个文件里后者覆盖前者
 * @param weight     身体重量（缺省不声明，沿用更早来源的重量）
 */
public record BodyPartFile(
    boolean overWrite,
    int priority,
    Identifier mobId,
    Optional<String> template,
    List<RawPart> parts,
    Optional<String> defaultPart,
    Optional<Double> weight) {

    public static final Codec<BodyPartFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("over_write", false).forGetter(BodyPartFile::overWrite),
        Codec.INT.optionalFieldOf("priority", 0).forGetter(BodyPartFile::priority),
        Identifier.CODEC.fieldOf("mob_id").forGetter(BodyPartFile::mobId),
        Codec.STRING.optionalFieldOf("template").forGetter(BodyPartFile::template),
        RawPart.CODEC.listOf().optionalFieldOf("parts", List.of()).forGetter(BodyPartFile::parts),
        Codec.STRING.optionalFieldOf("default_part").forGetter(BodyPartFile::defaultPart),
        Codec.DOUBLE.optionalFieldOf("weight").forGetter(BodyPartFile::weight))
        .apply(instance, BodyPartFile::new));

    public BodyPartFile {
        parts = List.copyOf(parts);
    }

    /**
     * 解析 {@link #template()} 引用的身体模板（先内置 Body，后数据包模板，见 {@link BodyTemplates#resolve}）。
     * <p>必须带命名空间；未写命名空间、格式非法或找不到对应模板时报告并忽略该引用
     * （本文件自定义的部位仍会照常应用）。</p>
     */
    public Optional<Body> templateBody(Identifier fileId, Consumer<String> warn) {
        if (this.template.isEmpty()) {
            return Optional.empty();
        }

        String raw = this.template.get();
        if (raw.indexOf(':') < 0) {
            warn.accept(fileId + ": template='" + raw + "' 缺少命名空间，需要写成 命名空间:名称（例如 "
                + Bodies.HUMAN_LIKE_ID + "）");
            return Optional.empty();
        }

        Identifier id = Identifier.tryParse(raw);
        if (id == null) {
            warn.accept(fileId + ": template='" + raw + "' 不是合法的注册名，已忽略");
            return Optional.empty();
        }

        Optional<Body> body = BodyTemplates.resolve(id);
        if (body.isEmpty()) {
            warn.accept(fileId + ": template='" + raw
                + "' 找不到对应的模板（内置 Body 或 newnew_body_template 都没有该 id），已忽略");
        }
        return body;
    }

    /**
     * 单个受击部位的原始数据。
     *
     * @param name        可读名称，缺省空串
     * @param surfaces    受击面数组，取值 {@code top}/{@code bottom}/{@code left}/{@code right}/{@code front}/{@code back}；
     *                    缺省为全身六个面，同一个部位会按数组顺序展开成多个面
     * @param point0      第一个角点在面内的坐标数组，缺省 {@code [0, 0]}；只读前 2 位，缺位补 0，多位忽略
     * @param point1      另一个角点在面内的坐标数组，缺省 {@code [1, 1]}，规则同上
     * @param damageScale 受伤倍率，缺省 1
     * @param rigidity    部位刚性，缺省 0.5（中刚性档）
     * @param armorSlot   保护该部位的装备槽，取值 {@code head}/{@code chest}/{@code legs}/{@code feet}，缺省无护甲
     * @param effects     受击效果数组，缺省为空
     */
    public record RawPart(
        String name,
        Optional<List<String>> surfaces,
        List<Float> point0,
        List<Float> point1,
        float damageScale,
        float rigidity,
        Optional<String> armorSlot,
        List<RawEffect> effects) {

        /** {@code point_0} 缺省角点 */
        private static final List<Float> DEFAULT_POINT_0 = List.of(0.0F, 0.0F);

        /** {@code point_1} 缺省角点 */
        private static final List<Float> DEFAULT_POINT_1 = List.of(1.0F, 1.0F);

        public static final Codec<RawPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("part_name", "").forGetter(RawPart::name),
            Codec.STRING.listOf().optionalFieldOf("surfaces").forGetter(RawPart::surfaces),
            Codec.FLOAT.listOf().optionalFieldOf("point_0", DEFAULT_POINT_0).forGetter(RawPart::point0),
            Codec.FLOAT.listOf().optionalFieldOf("point_1", DEFAULT_POINT_1).forGetter(RawPart::point1),
            Codec.FLOAT.optionalFieldOf("damage_scale", 1.0F).forGetter(RawPart::damageScale),
            Codec.FLOAT.optionalFieldOf("rigidity", BodyPart.DEFAULT_RIGIDITY).forGetter(RawPart::rigidity),
            Codec.STRING.optionalFieldOf("armor_slot").forGetter(RawPart::armorSlot),
            RawEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(RawPart::effects))
            .apply(instance, RawPart::new));

        public RawPart {
            point0 = List.copyOf(point0);
            point1 = List.copyOf(point1);
            effects = List.copyOf(effects);
        }

        /**
         * 解析成最终数据（一个原始部位可能按面展开成多个部位）。
         * <p>坐标数组只读前 2 位、缺位补 0；未知面名会逐个忽略并报告，
         * 无效的效果注册名会被跳过并报告。</p>
         *
         * @param fileId 数据文件 id（用于日志）
         * @param warn   警告输出（无效条目不会中断整个文件）
         * @return 解析出的部位（顺序与 {@code surfaces} 一致）；无法解析时为空列表
         */
        public List<BodyPart> resolve(Identifier fileId, Consumer<String> warn) {
            List<HitFace> faces = facesOf(fileId, warn);
            if (faces.isEmpty()) {
                return List.of();
            }

            List<BodyEffect> resolved = new ArrayList<>(this.effects.size());
            for (RawEffect raw : this.effects) {
                raw.resolve(fileId, this.name, warn).ifPresent(resolved::add);
            }

            float u0 = coordinate(this.point0, 0);
            float v0 = coordinate(this.point0, 1);
            float u1 = coordinate(this.point1, 0);
            float v1 = coordinate(this.point1, 1);
            EquipmentSlot slot = resolveArmorSlot(fileId, warn);

            List<BodyPart> parts = new ArrayList<>(faces.size());
            for (HitFace face : faces) {
                parts.add(new BodyPart(
                    this.name, face, u0, v0, u1, v1, this.damageScale, this.rigidity, slot, resolved));
            }
            return parts;
        }

        /**
         * 解析 {@code armor_slot}：缺省表示不受护甲保护；未知取值报告并忽略。
         */
        private EquipmentSlot resolveArmorSlot(Identifier fileId, Consumer<String> warn) {
            if (this.armorSlot.isEmpty()) {
                return null;
            }
            String raw = this.armorSlot.get();
            return switch (raw) {
                case "head" -> EquipmentSlot.HEAD;
                case "chest" -> EquipmentSlot.CHEST;
                case "legs" -> EquipmentSlot.LEGS;
                case "feet" -> EquipmentSlot.FEET;
                default -> {
                    warn.accept(fileId + ": part '" + this.name + "' 的 armor_slot='" + raw
                        + "' 无效（可用 head/chest/legs/feet），已忽略");
                    yield null;
                }
            };
        }

        /**
         * 解析 {@code surfaces}：缺省为全身六个面；未知面名逐个忽略并报告；
         * 重复的面只保留首次出现的位置（保证“按顺序取第一个命中”稳定）。
         */
        private List<HitFace> facesOf(Identifier fileId, Consumer<String> warn) {
            if (this.surfaces.isEmpty()) {
                return HitFace.ALL;
            }

            List<String> names = this.surfaces.get();
            if (names.isEmpty()) {
                warn.accept(fileId + ": part '" + this.name + "' 的 surfaces 为空数组，已跳过该部位");
                return List.of();
            }

            List<HitFace> faces = new ArrayList<>(names.size());
            for (String name : names) {
                Optional<HitFace> face = HitFace.byName(name);
                if (face.isEmpty()) {
                    warn.accept(fileId + ": part '" + this.name + "' 的 surfaces 含未知面 '" + name
                        + "'（可用 top/bottom/left/right/front/back），已忽略该项");
                    continue;
                }
                if (!faces.contains(face.get())) {
                    faces.add(face.get());
                }
            }

            if (faces.isEmpty()) {
                warn.accept(fileId + ": part '" + this.name + "' 的 surfaces 没有任何有效面，已跳过该部位");
            }
            return faces;
        }

        /** 读取坐标数组的第 {@code index} 位，缺位补 0 */
        private static float coordinate(List<Float> values, int index) {
            if (index >= values.size()) {
                return 0.0F;
            }
            Float value = values.get(index);
            return value == null ? 0.0F : value;
        }
    }

    /**
     * 单个受击效果的原始数据。
     *
     * @param effectId    效果的注册名，例如 {@code minecraft:poison}；缺省时该效果被跳过
     * @param duration    持续时间（游戏刻），缺省 0
     * @param amplifier   效果等级（原版 amplifier，0 表示 I 级），缺省 0
     * @param probability 触发概率基准，缺省 1
     */
    public record RawEffect(
        Optional<Identifier> effectId, int duration, int amplifier, float probability) {

        public static final Codec<RawEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("effect_id").forGetter(RawEffect::effectId),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(RawEffect::duration),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(RawEffect::amplifier),
            Codec.FLOAT.optionalFieldOf("probability", 1.0F).forGetter(RawEffect::probability))
            .apply(instance, RawEffect::new));

        /** 解析成最终数据；缺少或无效的 {@code effect_id} 会跳过该效果并报告 */
        public Optional<BodyEffect> resolve(
            Identifier fileId, String partName, Consumer<String> warn) {
            if (this.effectId.isEmpty()) {
                warn.accept(fileId + ": part '" + partName + "' 的某个效果缺少 effect_id，已跳过");
                return Optional.empty();
            }

            Optional<Holder.Reference<MobEffect>> holder =
                BuiltInRegistries.MOB_EFFECT.get(this.effectId.get());
            if (holder.isEmpty()) {
                warn.accept(fileId + ": part '" + partName + "' 的 effect_id="
                    + this.effectId.get() + " 未注册，已跳过");
                return Optional.empty();
            }

            return Optional.of(new BodyEffect(
                holder.get(), this.duration, this.amplifier, this.probability));
        }
    }
}
