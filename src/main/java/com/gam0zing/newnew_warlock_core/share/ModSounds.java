package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组音效注册表。
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, NewNewWarlockCore.MOD_ID);

    /** 天堂鸟振翅冲刺的音效 */
    public static final DeferredHolder<SoundEvent, SoundEvent> BIRD_FLY =
        SOUNDS.register("bird_fly",
            () -> SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "bird_fly")));

    private ModSounds() {}
}
