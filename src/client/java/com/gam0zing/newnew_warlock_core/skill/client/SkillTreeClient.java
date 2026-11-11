package com.gam0zing.newnew_warlock_core.skill.client;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * 客户端入口：技能树界面快捷键与客户端缓存清理。
 */
public final class SkillTreeClient {

    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "skill_tree"));

    public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping(
        "key.newnew_warlock_core.skill_tree",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYBOARD,
        InputConstants.KEY_K,
        CATEGORY);

    private SkillTreeClient() {}

    @EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID, value = Dist.CLIENT)
    public static final class ModEvents {

        private ModEvents() {}

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SKILL_TREE);
        }
    }

    @EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {

        private GameEvents() {}

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (OPEN_SKILL_TREE.consumeClick()) {
                Minecraft.getInstance().setScreenAndShow(new SkillTreeScreen());
            }
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            SkillTreeClientData.clear();
        }
    }
}
