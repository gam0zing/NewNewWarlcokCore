package com.gam0zing.newnew_warlock_core.active;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.active.net.FlashStepPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 闪身步的客户端输入采集。
 * <p>只负责识别移动方向键的双击并上报方向；是否释放由服务端判定。</p>
 * <p>双击拦截：7 tick 内再次按下同一个移动键才算双击，期间按下任何其他键盘按键都会作废。
 * 按键与 {@code options.keyUp/keyDown/keyLeft/keyRight} 比较，因此跟随键位绑定。</p>
 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID, value = Dist.CLIENT)
public final class FlashStepClient {

    private static final int DOUBLE_TAP_TICKS = 7;
    private static final byte NONE = -1;

    private static byte pendingDirection = NONE;
    private static int pendingTick;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.mouseHandler.isMouseGrabbed()) {
            return;
        }
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }

        InputConstants.Key pressed = InputConstants.getKey(event.getKeyEvent());
        byte direction = directionOf(minecraft, pressed);
        if (direction == NONE) {
            pendingDirection = NONE;
            return;
        }

        int now = minecraft.player.tickCount;
        if (pendingDirection == direction && now - pendingTick <= DOUBLE_TAP_TICKS) {
            pendingDirection = NONE;
            ClientPacketDistributor.sendToServer(new FlashStepPayload(direction));
        } else {
            pendingDirection = direction;
            pendingTick = now;
        }
    }

    private static byte directionOf(Minecraft minecraft, InputConstants.Key pressed) {
        if (pressed.equals(minecraft.options.keyUp.getKey())) {
            return 0;
        }
        if (pressed.equals(minecraft.options.keyDown.getKey())) {
            return 1;
        }
        if (pressed.equals(minecraft.options.keyLeft.getKey())) {
            return 2;
        }
        if (pressed.equals(minecraft.options.keyRight.getKey())) {
            return 3;
        }
        return NONE;
    }

    private FlashStepClient() {}
}
