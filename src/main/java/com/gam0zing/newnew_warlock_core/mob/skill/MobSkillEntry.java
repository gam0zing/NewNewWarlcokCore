package com.gam0zing.newnew_warlock_core.mob.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * 技能列表里的单个条目：技能 id、技能等级，以及该技能自己的状态容器 {@link #custom()}。
 * <p>{@code custom} 是开放结构，由技能实现类用自己的 MapCodec 解释。
 * 例如冷却技能在其中存 {@code cooldown}，持续技能再存 {@code duration}：
 * {@code {id: "...", level: 1, custom: {cooldown: 80, duration: 20}}}。</p>
 * <p>等级与状态存放在同一个条目里，技能系统与技能状态读的是同一份数据；
 * 技能被移出列表时状态也随之消失，不会留下孤立的状态数据。</p>
 * <p>{@code custom} 在 NBT 中可省略，旧数据（只有 id 和 level）会按空容器读取。
 * 技能池 {@link NewNewMobSkillPool#getRandom} 通常只需要 {@code id} 和 {@code level}，
 * 可用 {@link #MobSkillEntry(Identifier, int)} 构造。不要原地修改 {@code custom}，
 * 需要更新时通过 {@link MobSkills#setState} 替换整个条目。</p>
 */
public record MobSkillEntry(Identifier id, int level, CompoundTag custom) {

    public static final Codec<MobSkillEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(MobSkillEntry::id),
        Codec.INT.fieldOf("level").forGetter(MobSkillEntry::level),
        CompoundTag.CODEC.optionalFieldOf("custom", new CompoundTag()).forGetter(MobSkillEntry::custom))
        .apply(instance, MobSkillEntry::new));

    public MobSkillEntry {
        if (custom == null) {
            custom = new CompoundTag();
        }
    }

    /** 只带 id 和等级、状态为空的条目（技能池随机结果用）。 */
    public MobSkillEntry(Identifier id, int level) {
        this(id, level, new CompoundTag());
    }
}
