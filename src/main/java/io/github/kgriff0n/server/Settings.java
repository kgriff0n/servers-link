package io.github.kgriff0n.server;

public class Settings {

    private boolean playerList;
    private boolean chat;
    private boolean playerData;
    private boolean whitelist;
    private boolean roles;

    public Settings(boolean playerList, boolean chat, boolean playerData, boolean whitelist, boolean roles) {
        this.playerList = playerList;
        this.chat = chat;
        this.playerData = playerData;
        this.whitelist = whitelist;
        this.roles = roles;
    }

    public boolean isPlayerListSynced() {
        return playerList;
    }

    public boolean isChatSynced() {
        return chat;
    }

    public boolean isPlayerDataSynced() {
        return playerData;
    }

    public boolean isWhitelistSynced() {
        return whitelist;
    }

    public boolean isRolesSynced() {
        return roles;
    }

    public void setPlayerListSynced(boolean playerList) {
        this.playerList = playerList;
    }

    public void setChatSynced(boolean chat) {
        this.chat = chat;
    }

    public void setPlayerDataSynced(boolean playerData) {
        this.playerData = playerData;
    }

    public void setWhitelistSynced(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public void setRolesSynced(boolean roles) {
        this.roles = roles;
    }
}
