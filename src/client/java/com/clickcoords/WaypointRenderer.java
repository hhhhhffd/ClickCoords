package com.clickcoords;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/** Projects one temporary position through the same camera transform as vanilla waypoints. */
public final class WaypointRenderer {
    private static final int TEAL = 0xFF53E9E4;
    private static final int WHITE = 0xFFF2FCFC;
    private static final int PANEL = 0xDD121B26;
    private static final int BORDER = 0xE5060B11;
    private static final double MIN_FORWARD_DEPTH = 0.05;

    private WaypointRenderer() { }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("clickcoords", "waypoint"), WaypointRenderer::render);
    }

    private static void render(GuiGraphics g, DeltaTracker ignored) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        Waypoints.Marker marker = Waypoints.active(client);
        if (marker == null) return;

        Camera camera = client.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) return;

        Vec3 position = marker.position();
        Vec3 anchor = position.add(0.0, 1.5, 0.0);
        Vec3 relative = anchor.subtract(camera.position());
        double forward = dot(relative, camera.forwardVector());
        int width = g.guiWidth(), height = g.guiHeight();
        if (width < 32 || height < 32) return;

        long metres = Math.round(client.player.position().distanceTo(position));
        boolean russian = "ru_ru".equals(client.getLanguageManager().getSelected());
        String label = russian ? "Метка • " + metres + " м" : "Waypoint • " + metres + " m";
        double midX = width / 2.0, midY = height / 2.0;
        double marginX = Math.min(22.0, width / 4.0), marginY = Math.min(24.0, height / 4.0);

        if (forward > MIN_FORWARD_DEPTH) {
            Vec3 projected = client.gameRenderer.projectPointToScreen(anchor);
            double px = midX * (1.0 + projected.x);
            double py = midY * (1.0 - projected.y);
            if (Double.isFinite(px) && Double.isFinite(py)) {
                if (px >= marginX && px <= width - marginX && py >= marginY && py <= height - marginY) {
                    int x = (int) Math.round(px), y = (int) Math.round(py);
                    diamond(g, x, y);
                    plate(g, client, x, y - 22, label);
                    return;
                }
                edge(g, client, width, height, px - midX, py - midY, label, false);
                return;
            }
        }

        // Projection mirrors points behind the eye. Use camera basis directly for those.
        double right = -dot(relative, camera.leftVector());
        double down = -dot(relative, camera.upVector());
        boolean directlyBehind = Math.abs(right) + Math.abs(down) < 0.02;
        edge(g, client, width, height, right, directlyBehind ? -1.0 : down, label, directlyBehind);
    }

    private static void edge(GuiGraphics g, Minecraft client, int width, int height,
                             double dx, double dy, String label, boolean turnAround) {
        double middleX = width / 2.0, middleY = height / 2.0;
        double safeX = Math.max(1.0, middleX - 14.0);
        double safeY = Math.max(1.0, middleY - 22.0);
        double norm = Math.max(Math.abs(dx) / safeX, Math.abs(dy) / safeY);
        if (!(norm > 0.0) || !Double.isFinite(norm)) return;
        int x = (int) Math.round(middleX + dx / norm);
        int y = (int) Math.round(middleY + dy / norm);
        String arrow = turnAround ? "↶" : Math.abs(dx) / safeX >= Math.abs(dy) / safeY
                ? dx < 0 ? "←" : "→"
                : dy < 0 ? "↑" : "↓";
        g.fill(x - 7, y - 7, x + 8, y + 8, BORDER);
        g.fill(x - 6, y - 6, x + 7, y + 7, PANEL);
        g.drawString(client.font, arrow, x - client.font.width(arrow) / 2, y - 4, TEAL, true);
        plate(g, client, x, y <= middleY ? y + 10 : y - 24, label);
    }

    private static void diamond(GuiGraphics g, int x, int y) {
        for (int row = -6; row <= 6; ++row) {
            int half = 6 - Math.abs(row);
            g.fill(x - half, y + row, x + half + 1, y + row + 1, BORDER);
        }
        for (int row = -4; row <= 4; ++row) {
            int half = 4 - Math.abs(row);
            g.fill(x - half, y + row, x + half + 1, y + row + 1, TEAL);
        }
        g.fill(x - 1, y - 1, x + 2, y + 2, WHITE);
    }

    private static void plate(GuiGraphics g, Minecraft client, int x, int y, String label) {
        int maximumTextWidth = Math.max(1, g.guiWidth() - 16);
        if (client.font.width(label) > maximumTextWidth) {
            label = client.font.plainSubstrByWidth(label, maximumTextWidth);
        }
        int nameWidth = client.font.width(label);
        int boxWidth = nameWidth + 10;
        int left = Math.max(3, Math.min(g.guiWidth() - boxWidth - 3, x - boxWidth / 2));
        int top = Math.max(2, Math.min(g.guiHeight() - 17, y));
        g.fill(left, top, left + boxWidth, top + 14, PANEL);
        g.fill(left, top, left + 2, top + 14, TEAL);
        g.drawString(client.font, label, left + 6, top + 3, WHITE, true);
    }

    private static double dot(Vec3 p, Vector3fc v) {
        return p.x * v.x() + p.y * v.y() + p.z * v.z();
    }
}
