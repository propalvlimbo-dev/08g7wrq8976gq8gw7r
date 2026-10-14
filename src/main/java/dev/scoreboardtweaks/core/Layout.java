package dev.scoreboardtweaks.core;

/** GUI-scaled coordinates, independent of Minecraft for deterministic tests. */
public final class Layout {
    private Layout() {}

    public static double unit(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }

    public static int clamp(int value, int extent, int screen) {
        return Math.max(0, Math.min(Math.max(0, screen - extent), value));
    }

    public static int fromAnchor(double anchor, int extent, int screen) {
        return (int) Math.round(unit(anchor) * Math.max(0, screen - extent));
    }

    public static double toAnchor(double coordinate, int extent, int screen) {
        int room = screen - extent;
        return room > 0 ? unit(coordinate / room) : 0.0;
    }

    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
}
