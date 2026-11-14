package com.gam0zing.newnew_warlock_core.mob.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import com.gam0zing.newnew_warlock_core.mob.MobKillPointsDisplay;

/**
 * 击杀生物获得属性点时的浮动提示：屏幕底部居中、快捷栏上方，向上漂浮并淡出。
 * <p>多条提示最多同时保留 {@value #MAX_POPUPS} 条；{@value #MERGE_WINDOW_MS} 毫秒内的连续击杀
 * 合并成一条并累加数值，避免范围击杀时刷屏。</p>
 * <p>颜色按双方 points 比较得出的缩放：大于 1 金色、等于 1 白色、小于 1 灰色。</p>
 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID, value = Dist.CLIENT)
public final class MobKillPointsHud {

    /** 单条提示的总存在时长 */
    private static final long LIFETIME_MS = 1200L;
    /** 连续获取的合并窗口 */
    private static final long MERGE_WINDOW_MS = 500L;
    private static final long FADE_IN_MS = 150L;
    private static final long FADE_OUT_MS = 400L;
    private static final int MAX_POPUPS = 3;
    /** 整条生命周期内向上漂浮的像素 */
    private static final double RISE_PIXELS = 10.0D;
    /** 基准行距离屏幕底部的像素 */
    private static final int BOTTOM_MARGIN = 55;
    private static final int LINE_HEIGHT = 10;

    private static final int COLOR_GOLD = 0xFFFFD24A;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GRAY = 0xFFAAAAAA;

    private static final List<Popup> POPUPS = new ArrayList<>();

    private MobKillPointsHud() {}

    /** 一条正在播放的提示；连续获取会累加到最新一条上 */
    private static final class Popup {
        private double gained;
        private double scale;
        private long start;

        private Popup(double gained, double scale, long start) {
            this.gained = gained;
            this.scale = scale;
            this.start = start;
        }
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "mob_kill_points_hud"),
            MobKillPointsHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            POPUPS.clear();
            return;
        }

        long now = System.currentTimeMillis();
        drain(now);
        POPUPS.removeIf(popup -> now - popup.start >= LIFETIME_MS);
        if (POPUPS.isEmpty()) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int baseY = graphics.guiHeight() - BOTTOM_MARGIN;
        for (int index = 0; index < POPUPS.size(); index++) {
            Popup popup = POPUPS.get(index);
            long age = now - popup.start;
            int color = withAlpha(colorFor(popup.scale), alpha(age));
            Component text = Component.translatable(
                "hud.newnew_warlock_core.kill_points", format(popup.gained));
            int x = centerX - minecraft.font.width(text) / 2;
            int y = (int) Math.round(baseY - index * LINE_HEIGHT - RISE_PIXELS * age / LIFETIME_MS);
            graphics.text(minecraft.font, text, x, y, color);
        }
    }

    /** 把网络包放进来的记录转成动画条目，并合并短时间内的连续获取。 */
    private static void drain(long now) {
        MobKillPointsDisplay.Gain gain;
        while ((gain = MobKillPointsDisplay.poll()) != null) {
            Popup newest = POPUPS.isEmpty() ? null : POPUPS.get(POPUPS.size() - 1);
            if (newest != null && now - newest.start <= MERGE_WINDOW_MS) {
                newest.gained += gain.gained();
                newest.scale = gain.scale();
                newest.start = now;
                continue;
            }
            POPUPS.add(new Popup(gain.gained(), gain.scale(), now));
            while (POPUPS.size() > MAX_POPUPS) {
                POPUPS.remove(0);
            }
        }
    }

    private static float alpha(long age) {
        if (age < FADE_IN_MS) {
            return clamp01(age / (float) FADE_IN_MS);
        }
        long fadeStart = LIFETIME_MS - FADE_OUT_MS;
        if (age > fadeStart) {
            return clamp01((LIFETIME_MS - age) / (float) FADE_OUT_MS);
        }
        return 1.0F;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static int colorFor(double scale) {
        if (scale > 1.0D) {
            return COLOR_GOLD;
        }
        return scale < 1.0D ? COLOR_GRAY : COLOR_WHITE;
    }

    private static int withAlpha(int color, float alpha) {
        int alphaBits = Math.round(alpha * 255.0F) << 24;
        return alphaBits | (color & 0x00FFFFFF);
    }

    private static String format(double gained) {
        return String.format(Locale.ROOT, "%.2f", gained);
    }
}
