package com.clickcoords;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class ClickCoordsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WaypointRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(Waypoints::active);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> Waypoints.clear());
    }
}
