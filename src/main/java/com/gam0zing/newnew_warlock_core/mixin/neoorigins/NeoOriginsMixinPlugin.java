package com.gam0zing.newnew_warlock_core.mixin.neoorigins;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.loading.LoadingModList;

/**
 * 仅在 NeoOrigins 存在时应用本配置里的 mixin。
 * <p>NeoOrigins 是可选依赖，缺少它时不能让这些 mixin 去解析目标类。</p>
 */
public final class NeoOriginsMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        try {
            LoadingModList modList = LoadingModList.get();
            return modList != null && modList.getModFileById("neoorigins") != null;
        } catch (RuntimeException ignored) {
            // 加载早期拿不到模块列表；配置本身 required=false，目标缺失时会自动跳过。
            return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
