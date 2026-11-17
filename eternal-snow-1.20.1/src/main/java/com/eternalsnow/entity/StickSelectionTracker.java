package com.eternalsnow.entity;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StickSelectionTracker {
    private static final Map<UUID, Integer> SELECTED_GOLEM_ENTITY_ID = new ConcurrentHashMap<>();

    private StickSelectionTracker() {
    }

    public static void selectGolem(UUID playerUuid, int golemEntityId) {
        SELECTED_GOLEM_ENTITY_ID.put(playerUuid, golemEntityId);
    }

    public static Integer getSelectedGolemId(UUID playerUuid) {
        return SELECTED_GOLEM_ENTITY_ID.get(playerUuid);
    }

    public static Integer consumeSelectedGolem(ServerPlayer player) {
        return SELECTED_GOLEM_ENTITY_ID.remove(player.getUUID());
    }

    /** Drops the picked golem for one player — called on logout so the map cannot outlive a session. */
    public static void clearPlayer(UUID playerUuid) {
        SELECTED_GOLEM_ENTITY_ID.remove(playerUuid);
    }

    public static void clearAll() {
        SELECTED_GOLEM_ENTITY_ID.clear();
    }
}
