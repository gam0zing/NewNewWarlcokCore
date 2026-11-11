package com.gam0zing.newnew_warlock_core.body.hit;

import net.minecraft.util.Mth;

/**
 * 一次受击的位置。
 *
 * @param face 受击面（生物自身的相对方向，见 {@link HitFace}）
 * @param x    该面上的横坐标，以“从体外正对该面看”的左下角为 (0, 0)，右上角为 (1, 1)
 * @param y    该面上的纵坐标，同上
 */
public record HitLocation(HitFace face, float x, float y) {

    public HitLocation {
        x = Mth.clamp(x, 0.0F, 1.0F);
        y = Mth.clamp(y, 0.0F, 1.0F);
    }

    /** {@return 受击面的数值（0-5），便于序列化} */
    public int faceIndex() {
        return this.face.index();
    }
}
