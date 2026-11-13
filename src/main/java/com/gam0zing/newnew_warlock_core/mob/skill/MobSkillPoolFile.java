package com.gam0zing.newnew_warlock_core.mob.skill;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * 一份 {@code data/<命名空间>/mob_skill_pool/<文件名>.json} 数据文件。
 * <p>同一种生物可以有多个文件，按文件 id 升序应用；{@link #overWrite()} 为 true 时
 * 先清空此前累积的技能池列表（包括内置绑定）再追加，否则直接追加。</p>
 *
 * @param overWrite 是否覆盖此前已累积的技能池（而不是追加）
 * @param mobId     生物的注册名，例如 {@code minecraft:zombie}
 * @param pools     技能池注册名列表；同一文件内的重复 id 只保留首次出现
 */
public record MobSkillPoolFile(boolean overWrite, Identifier mobId, List<Identifier> pools) {

    public static final Codec<MobSkillPoolFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("over_write", false).forGetter(MobSkillPoolFile::overWrite),
        Identifier.CODEC.fieldOf("mob_id").forGetter(MobSkillPoolFile::mobId),
        Identifier.CODEC.listOf().fieldOf("pools").forGetter(MobSkillPoolFile::pools))
        .apply(instance, MobSkillPoolFile::new));

    public MobSkillPoolFile {
        pools = List.copyOf(pools);
    }
}
