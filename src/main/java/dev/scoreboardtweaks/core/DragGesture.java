package dev.scoreboardtweaks.core;

/** A click becomes a drag only after travelling 3 GUI pixels. */
public final class DragGesture {
    public enum Release { NONE, CLICK, DRAG }
    private boolean pressed;
    private boolean dragging;
    private double startX, startY, offsetX, offsetY;

    public boolean press(double x, double y, Layout.Bounds bounds) {
        if (pressed || bounds == null || !bounds.contains(x, y)) return false;
        pressed = true;
        dragging = false;
        startX = x;
        startY = y;
        offsetX = x - bounds.x();
        offsetY = y - bounds.y();
        return true;
    }

    public boolean move(double x, double y) {
        if (!pressed) return false;
        double dx = x - startX, dy = y - startY;
        if (dx * dx + dy * dy >= 9.0) dragging = true;
        return dragging;
    }

    public Release release(double x, double y) {
        if (!pressed) return Release.NONE;
        move(x, y);
        Release result = dragging ? Release.DRAG : Release.CLICK;
        cancel();
        return result;
    }

    public boolean isPressed() { return pressed; }
    public boolean isDragging() { return dragging; }
    public double left(double mouseX) { return mouseX - offsetX; }
    public double top(double mouseY) { return mouseY - offsetY; }
    public void cancel() { pressed = false; dragging = false; }
}
