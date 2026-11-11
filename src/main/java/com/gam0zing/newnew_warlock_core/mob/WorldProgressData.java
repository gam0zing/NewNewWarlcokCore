package com.gam0zing.newnew_warlock_core.mob;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

/**
 * 世界进度标记：本存档是否成形过下界/末地传送门。
 * <p>存在主世界（overworld）的 {@code data/newnew_warlock_core_world_progress.dat}。
 * 用于 {@code difficulty_global.toml} 的“开启过传送门”难度加成，只在主世界生效。</p>
 */
public final class WorldProgressData extends SavedData {

    private static final String NETHER_OPENED = "nether_opened";
    private static final String END_OPENED = "end_opened";

    public static final Codec<WorldProgressData> CODEC =
        CompoundTag.CODEC.xmap(WorldProgressData::fromTag, WorldProgressData::toTag);

    public static final SavedDataType<WorldProgressData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "world_progress"),
        WorldProgressData::new,
        CODEC);

    private boolean netherOpened;
    private boolean endOpened;

    /** 读取（必要时创建）主世界上的进度标记。 */
    public static WorldProgressData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean netherOpened() {
        return this.netherOpened;
    }

    public boolean endOpened() {
        return this.endOpened;
    }

    public void markNetherOpened() {
        if (!this.netherOpened) {
            this.netherOpened = true;
            this.setDirty();
        }
    }

    public void markEndOpened() {
        if (!this.endOpened) {
            this.endOpened = true;
            this.setDirty();
        }
    }

    private static WorldProgressData fromTag(CompoundTag tag) {
        WorldProgressData data = new WorldProgressData();
        data.netherOpened = tag.getBooleanOr(NETHER_OPENED, false);
        data.endOpened = tag.getBooleanOr(END_OPENED, false);
        return data;
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(NETHER_OPENED, this.netherOpened);
        tag.putBoolean(END_OPENED, this.endOpened);
        return tag;
    }
}
