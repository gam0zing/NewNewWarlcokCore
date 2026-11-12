package com.gam0zing.newnew_warlock_core.enchant;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

/**
 * 附魔相关的一次性世界解锁：四个 Boss/事件的首次胜利。
 * <p>存在主世界的 {@code data/newnew_warlock_core_enchant_unlocks.dat}，存档内所有玩家共享、重启后保留。</p>
 * <p>{@link #active} 是当前服务端数据的快照，供没有 {@link ServerLevel} 参数的数值计算路径
 * （附魔台 power、基准最大等级、铁砧费用）静态读取；服务端未加载时所有加成都为 0，因此不影响默认行为。</p>
 */
public final class EnchantUnlockData extends SavedData {

    /** 首次击杀凋零后，附魔台 power 的额外加成。 */
    public static final int WITHER_EXTRA_POWER = 5;
    /** 首次击杀末影龙后，基准最大等级的额外加成。 */
    public static final int DRAGON_MAX_LEVEL_BONUS = 1;

    private static final String RAID_DEFEATED = "raid_defeated";
    private static final String WARDEN_DEFEATED = "warden_defeated";
    private static final String WITHER_DEFEATED = "wither_defeated";
    private static final String DRAGON_DEFEATED = "dragon_defeated";

    public static final Codec<EnchantUnlockData> CODEC =
        CompoundTag.CODEC.xmap(EnchantUnlockData::fromTag, EnchantUnlockData::toTag);

    public static final SavedDataType<EnchantUnlockData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "enchant_unlocks"),
        EnchantUnlockData::new,
        CODEC);

    private static volatile EnchantUnlockData active;

    private boolean raidDefeated;
    private boolean wardenDefeated;
    private boolean witherDefeated;
    private boolean dragonDefeated;

    /** 读取（必要时创建）主世界上的解锁记录，并刷新静态快照。 */
    public static EnchantUnlockData get(ServerLevel level) {
        EnchantUnlockData data = level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
        active = data;
        return data;
    }

    /** 服务端启动时载入存档数据。 */
    public static void activate(MinecraftServer server) {
        active = server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** 服务端停止时清空快照，避免下一个存档沿用旧值。 */
    public static void deactivate() {
        active = null;
    }

    public static boolean raidDefeated() {
        EnchantUnlockData data = active;
        return data != null && data.raidDefeated;
    }

    public static boolean wardenDefeated() {
        EnchantUnlockData data = active;
        return data != null && data.wardenDefeated;
    }

    /** 首次击杀凋零后的附魔台额外 power；未解锁为 0。 */
    public static int extraTablePower() {
        EnchantUnlockData data = active;
        return data != null && data.witherDefeated ? WITHER_EXTRA_POWER : 0;
    }

    /** 首次击杀末影龙后的基准最大等级加成；未解锁为 0。 */
    public static int maxLevelBonus() {
        EnchantUnlockData data = active;
        return data != null && data.dragonDefeated ? DRAGON_MAX_LEVEL_BONUS : 0;
    }

    public void markRaidDefeated() {
        if (!this.raidDefeated) {
            this.raidDefeated = true;
            this.setDirty();
        }
    }

    public void markWardenDefeated() {
        if (!this.wardenDefeated) {
            this.wardenDefeated = true;
            this.setDirty();
        }
    }

    public void markWitherDefeated() {
        if (!this.witherDefeated) {
            this.witherDefeated = true;
            this.setDirty();
        }
    }

    public void markDragonDefeated() {
        if (!this.dragonDefeated) {
            this.dragonDefeated = true;
            this.setDirty();
        }
    }

    private static EnchantUnlockData fromTag(CompoundTag tag) {
        EnchantUnlockData data = new EnchantUnlockData();
        data.raidDefeated = tag.getBooleanOr(RAID_DEFEATED, false);
        data.wardenDefeated = tag.getBooleanOr(WARDEN_DEFEATED, false);
        data.witherDefeated = tag.getBooleanOr(WITHER_DEFEATED, false);
        data.dragonDefeated = tag.getBooleanOr(DRAGON_DEFEATED, false);
        return data;
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(RAID_DEFEATED, this.raidDefeated);
        tag.putBoolean(WARDEN_DEFEATED, this.wardenDefeated);
        tag.putBoolean(WITHER_DEFEATED, this.witherDefeated);
        tag.putBoolean(DRAGON_DEFEATED, this.dragonDefeated);
        return tag;
    }
}
