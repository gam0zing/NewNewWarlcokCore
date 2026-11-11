package com.gam0zing.newnew_warlock_core.mob.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.mob.DifficultyDisplay;

/**
 * 左上角 HUD：显示当前位置的总难度（生物重建使用的那个难度）。
 * <p>数值由服务端每 tick 计算并同步（{@link com.gam0zing.newnew_warlock_core.mob.net.DifficultySync}），
 * 因此包含游戏天数、垂直/水平距离、传送门进度、维度与群系等全部因素，客户端不做本地估算。</p>
 * <p>颜色：不超过 15 白字，高于 15 黄字，高于 30 红字。</p>
 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID, value = Dist.CLIENT)
public final class DifficultyHud {

    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_YELLOW = 0xFFFFFF55;
    private static final int COLOR_RED = 0xFFFF5555;

    private DifficultyHud() {}

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "difficulty_hud"),
            DifficultyHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        double difficulty = DifficultyDisplay.get();
        if (minecraft.player == null || Double.isNaN(difficulty)) {
            return;
        }

        int color = difficulty > 30.0D ? COLOR_RED : difficulty > 15.0D ? COLOR_YELLOW : COLOR_WHITE;
        Component text = Component.translatable(
            "hud.newnew_warlock_core.difficulty", String.format("%.1f", difficulty));
        graphics.text(minecraft.font, text, 4, 4, color);
    }
}
