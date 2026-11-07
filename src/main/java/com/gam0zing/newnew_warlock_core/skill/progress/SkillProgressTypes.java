package com.gam0zing.newnew_warlock_core.skill.progress;

import java.util.function.Supplier;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 本模组使用的实体附件。
 */
public final class SkillProgressTypes {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NewNewWarlockCore.MOD_ID);

    /** 玩家技能树进度：随玩家 NBT 持久化，死亡保留 */
    public static final Supplier<AttachmentType<SkillProgress>> SKILL_PROGRESS =
        ATTACHMENT_TYPES.register("skill_progress", () -> AttachmentType.builder(SkillProgress::new)
            .serialize(SkillProgress.CODEC.fieldOf("skill_progress"))
            .copyOnDeath()
            .build());

    private SkillProgressTypes() {}
}
