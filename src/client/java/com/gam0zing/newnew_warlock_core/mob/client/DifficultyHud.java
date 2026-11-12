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
 * <p>显示格式为「难度(偏移)」，例如 {@code 50.0(+12.0)}：难度是不含服务器游戏难度偏移的原始值，
 * 括号内是服务器游戏难度带来的固定偏移（简单 -12 / 普通 0 / 困难 +12），仅用于参考，
 * 不影响难度本身的颜色判定。</p>
 * <p>颜色阈值取服务端下发的通用难度档位（默认中档 20、高档 40）：低于中档白字，达到中档黄字，
 * 达到高档红字。</p>
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

        int color = difficulty >= DifficultyDisplay.getHighThreshold() ? COLOR_RED
            : difficulty >= DifficultyDisplay.getMediumThreshold() ? COLOR_YELLOW
            : COLOR_WHITE;
        String value = String.format("%.1f(%+.1f)", difficulty, DifficultyDisplay.getOffset());
        Component text = Component.translatable("hud.newnew_warlock_core.difficulty", value);
        graphics.text(minecraft.font, text, 4, 4, color);
    }
}
