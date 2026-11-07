package com.gam0zing.newnew_warlock_core.skill.tree;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;

/**
 * 服务端生效的技能树快照（权威数据）。
 * <p>由 {@link SkillTreeLoader} 在资源重载时整体替换；客户端不复用这份状态，见
 * {@code SkillTreeClientData}。单机下客户端与服务端共享同一份静态状态，因此客户端不得清空这里。
 * 读侧无锁。</p>
 */
public final class SkillTrees {

    private static volatile Map<Identifier, SkillTreeDefinition> trees = Map.of();

    private SkillTrees() {}

    public static void replaceWith(Map<Identifier, SkillTreeDefinition> loaded) {
        Map<Identifier, SkillTreeDefinition> copy = new LinkedHashMap<>(loaded);
        trees = Map.copyOf(copy);
    }

    public static Map<Identifier, SkillTreeDefinition> all() {
        return trees;
    }

    @Nullable
    public static SkillTreeDefinition get(Identifier id) {
        return trees.get(id);
    }
}
