package com.clickcoords;

import java.time.Duration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** One local, temporary marker. It never uses the clipboard or sends chat. */
public final class Waypoints {
    private static final long LIFETIME_NS = Duration.ofMinutes(5).toNanos();
    private static Marker marker;

    public record Marker(Vec3 position, ClientLevel level, long placedAt, boolean horizontalOnly) {}

    private Waypoints() {}

    public static Marker active(Minecraft client) {
        if (marker != null && (client.player == null || marker.level != client.level
                || System.nanoTime() - marker.placedAt >= LIFETIME_NS)) {
            marker = null;
        }
        return marker;
    }

    public static void clear() {
        marker = null;
    }

    public static boolean toggle(Minecraft client, String coordinates) {
        if (client.level == null || client.player == null) return false;
        String[] xyz = coordinates.split(" ", -1);
        boolean horizontal = xyz.length == 2;
        if (!horizontal && xyz.length != 3) return false;
        try {
            var point = new Vec3(Double.parseDouble(xyz[0]), horizontal ? client.player.getBlockY() : Double.parseDouble(xyz[1]),
                    Double.parseDouble(xyz[xyz.length - 1]));
            if (!valid(point.x) || !valid(point.y) || !valid(point.z)) return false;
            Marker old = active(client);
            boolean same = old != null && old.horizontalOnly == horizontal && (horizontal
                    ? old.position.x == point.x && old.position.z == point.z : old.position.equals(point));
            marker = same ? null : new Marker(point, client.level, System.nanoTime(), horizontal);
            return true;
        } catch (NumberFormatException invalid) {
            return false;
        }
    }

    private static boolean valid(double coordinate) {
        return Double.isFinite(coordinate) && Math.abs(coordinate) <= 30_000_000;
    }
}
