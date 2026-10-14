package dev.scoreboardtweaks.core;

/** GUI-scaled coordinates, independent of Minecraft for deterministic tests. */
public final class Layout {
    private Layout() {}

    public static double unit(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 0.0;
    }

    public static double scale(double value) {
        return Double.isFinite(value) ? Math.max(0.5, Math.min(2.0, value)) : 1.0;
    }

    public static double fitScale(double requested, int width, int height, int screenWidth, int screenHeight) {
        return Math.min(scale(requested), Math.min(Math.max(1, screenWidth) / (double)Math.max(1, width),
            Math.max(1, screenHeight) / (double)Math.max(1, height)));
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
        public Bounds expanded(int margin) {
            int padding = Math.max(0, margin);
            return new Bounds(x - padding, y - padding, width + padding * 2, height + padding * 2);
        }
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
}
