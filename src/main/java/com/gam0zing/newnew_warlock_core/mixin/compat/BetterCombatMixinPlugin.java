package com.gam0zing.newnew_warlock_core.mixin.compat;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;

/**
 * Better Combat 兼容 mixin 的条件开关。
 * <p>Better Combat 只作为 {@code compileOnly} + {@code localRuntime} 的可选依赖存在，
 * 未安装时这整份 mixin 配置必须完全不应用，否则目标类缺失会导致启动失败。</p>
 */
public final class BetterCombatMixinPlugin implements IMixinConfigPlugin {

    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";

    private static boolean betterCombatLoaded() {
        FMLLoader loader = FMLLoader.getCurrentOrNull();
        if (loader == null) {
            return false;
        }
        LoadingModList modList = loader.getLoadingModList();
        return modList != null && modList.getModFileById(BETTER_COMBAT_MOD_ID) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return betterCombatLoaded();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
