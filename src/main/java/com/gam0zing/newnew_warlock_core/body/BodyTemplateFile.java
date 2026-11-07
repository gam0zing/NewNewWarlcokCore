package com.gam0zing.newnew_warlock_core.body;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.gam0zing.newnew_warlock_core.body.BodyPartFile.RawPart;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * 一份 newnew_body_template 数据文件
 * （{@code data/<命名空间>/newnew_body_template/<文件名>.json}）。
 *
 * <p><b>模板 id = 命名空间 + 文件名</b>（不含 {@code .json}）：
 * {@code data/foo/newnew_body_template/my_human.json} 的 id 是 {@code foo:my_human}。
 * 文件必须直接放在 {@code newnew_body_template} 的根目录下，放进子目录的文件会被跳过。</p>
 *
 * <p><b>模板不支持继承</b>：每个模板都独立、完整地定义自己的部位。文件里只有 {@code parts} 与 {@code weight}，
 * 没有 {@code template}、{@code priority}、{@code over_write}、{@code mob_id}、{@code default_part}
 * （出现这些字段会告警并且不生效）。模板由 newnew_body 数据文件的 {@code template} 字段引用，
 * 引用时必须写完整命名空间，例如 {@code "template": "foo:my_human"}。</p>
 *
 * <p>模板内部同样遵循“同一面上同名部位由后者整体替换前者”（见 {@link BodyParts#upsertPart}）。</p>
 *
 * @param parts  受击部位数组（缺省为空），格式与 newnew_body 的 {@code parts} 完全一致
 * @param weight 身体重量（缺省 1，非有限值或非正数按默认值处理）
 */
public record BodyTemplateFile(List<RawPart> parts, double weight) {

    public static final Codec<BodyTemplateFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RawPart.CODEC.listOf().optionalFieldOf("parts", List.of()).forGetter(BodyTemplateFile::parts),
        Codec.DOUBLE.optionalFieldOf("weight", Body.DEFAULT_WEIGHT).forGetter(BodyTemplateFile::weight))
        .apply(instance, BodyTemplateFile::new));

    public BodyTemplateFile {
        parts = List.copyOf(parts);
        weight = Double.isFinite(weight) && weight > 0.0D ? weight : Body.DEFAULT_WEIGHT;
    }

    /**
     * 解析成 {@link Body}，名字取模板 id 的路径部分（例如 {@code foo:my_human} → {@code my_human}）。
     *
     * @param templateId 模板 id（用于名字与日志）
     * @param warn       警告输出（无效条目只跳过自身，不中断整个文件）
     * @return 解析出的 Body（部位顺序与文件中的 {@code parts} 一致）
     */
    public Body resolve(Identifier templateId, Consumer<String> warn) {
        List<BodyPart> resolved = new ArrayList<>(this.parts.size());
        for (RawPart raw : this.parts) {
            for (BodyPart part : raw.resolve(templateId, warn)) {
                BodyParts.upsertPart(resolved, part);
            }
        }
        return new Body(templateId.getPath(), List.copyOf(resolved), this.weight);
    }
}
