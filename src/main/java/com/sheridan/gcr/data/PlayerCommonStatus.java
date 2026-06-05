package com.sheridan.gcr.data;

import com.sheridan.gcr.INBTSync;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class PlayerCommonStatus implements INBTSync {
    public static final PlayerCommonStatus EMPTY;

    static {
        EMPTY = new PlayerCommonStatus();
        EMPTY.gcrCredit = -1L;
        EMPTY.reloading = false;
        EMPTY.dataChanged = false;
    }

    public PlayerCommonStatus() {}

    //do not save
    public boolean reloading;
    //save
    public long gcrCredit;
    //sync to client and send broadcast? save
    public boolean dataChanged;

    public long getGcrCredit() {
        return gcrCredit;
    }

    public void serverSetGcrCredit(long balance) {
        if (balance >= 0 && balance != this.gcrCredit) {
            this.gcrCredit = balance;
            dataChanged = true;
        }
    }

    public void setGcrCredit(long gcrCredit) {
        this.gcrCredit = gcrCredit;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void setReloading(boolean reloading) {
        if (reloading != this.reloading) {
            this.reloading = reloading;
            dataChanged = true;
        }
    }

    public void copyFrom(PlayerCommonStatus oldData) {
        serverSetGcrCredit(oldData.getGcrCredit());
    }

    @Override
    public void writeData(CompoundTag tag) {
        tag.putLong("gcr_credit", gcrCredit);
    }

    @Override
    public void loadData(CompoundTag tag) {
        reloading = false;
        dataChanged = true;
        if (tag.contains("gcr_credit")) {
            gcrCredit = tag.getLong("gcr_credit");
        } else {
            gcrCredit = 0;
        }
    }

    public static boolean isReloading(Player player) {
        return player.hasData(ModData.PLAYER_STATUS) && player.getData(ModData.PLAYER_STATUS).isReloading();
    }

    public static long getGcrCredit(Player player) {
        return player.hasData(ModData.PLAYER_STATUS) ? player.getData(ModData.PLAYER_STATUS).getGcrCredit() : 0;
    }
}
