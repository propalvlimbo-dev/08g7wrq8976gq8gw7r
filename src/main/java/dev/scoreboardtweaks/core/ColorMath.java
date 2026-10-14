package dev.scoreboardtweaks.core;

/** HSV palette math, independent of the game and AWT. */
public final class ColorMath {
    private ColorMath() {}
    public static int rgb(double hue, double saturation, double value) {
        double h = Double.isFinite(hue) ? hue - Math.floor(hue) : 0;
        double s = Layout.unit(saturation), v = Layout.unit(value);
        double sector = h * 6;
        double fraction = sector - Math.floor(sector);
        double p = v * (1 - s), q = v * (1 - s * fraction), t = v * (1 - s * (1 - fraction));
        double r, g, b;
        switch ((int) sector) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int)Math.round(r * 255) << 16) | ((int)Math.round(g * 255) << 8) | (int)Math.round(b * 255);
    }
    public static double[] hsv(int rgb) {
        double r = ((rgb >> 16) & 255) / 255.0, g = ((rgb >> 8) & 255) / 255.0, b = (rgb & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
        double h = 0;
        if (delta > 0) {
            if (max == r) h = (g - b) / delta;
            else if (max == g) h = (b - r) / delta + 2;
            else h = (r - g) / delta + 4;
            h = (h / 6 + 1) % 1;
        }
        return new double[] { h, max == 0 ? 0 : delta / max, max };
    }
}
