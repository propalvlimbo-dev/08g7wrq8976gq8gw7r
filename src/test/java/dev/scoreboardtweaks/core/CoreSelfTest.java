package dev.scoreboardtweaks.core;

public final class CoreSelfTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(Layout.unit(Double.NaN) == 0, "NaN safe");
        check(Layout.unit(Double.POSITIVE_INFINITY) == 0, "infinity safe");
        check(Layout.unit(-2) == 0, "negative safe");
        check(Layout.unit(2) == 1, "upper bound");
        check(Layout.clamp(-20, 100, 300) == 0, "left edge");
        check(Layout.clamp(999, 100, 300) == 200, "right edge");
        check(Layout.clamp(25, 500, 300) == 0, "oversized board");
        check(Layout.toAnchor(99, 300, 300) == 0, "no division by zero");
        check(Layout.fromAnchor(1, 100, 500) == 400, "right anchor");
        check(Layout.fromAnchor(.5, 100, 500) == 200, "center anchor");
        check(Layout.fromAnchor(.5, 100, 900) == 400, "resize preserves relative position");
        for (int width : new int[] {320, 640, 960, 1920}) {
            for (int x = 0; x <= width - 120; x += 17) {
                double anchor = Layout.toAnchor(x, 120, width);
                check(Layout.fromAnchor(anchor, 120, width) == x, "position round trip " + width + ":" + x);
            }
        }
        Layout.Bounds bounds = new Layout.Bounds(10, 20, 100, 60);
        check(bounds.contains(10, 20), "top left included");
        check(!bounds.contains(110, 20), "right excluded");
        check(!bounds.contains(10, 80), "bottom excluded");
        check(!bounds.contains(9.9, 20), "outside left");
        DragGesture gesture = new DragGesture();
        check(!gesture.press(30, 30, null), "no board no capture");
        check(!gesture.press(0, 0, bounds), "outside click passes through");
        check(gesture.release(0, 0) == DragGesture.Release.NONE, "unowned release passes through");
        check(gesture.press(15, 25, bounds), "inside press captures");
        check(!gesture.press(15, 25, bounds), "no duplicate press");
        check(!gesture.move(16, 26), "tiny jitter not a drag");
        check(gesture.release(16, 26) == DragGesture.Release.CLICK, "jitter is click");
        check(!gesture.isPressed(), "click releases capture");
        check(gesture.press(15, 25, bounds), "second press");
        check(gesture.move(18, 25), "3 pixels starts drag");
        check(gesture.left(55) == 50, "grab offset X retained");
        check(gesture.top(65) == 60, "grab offset Y retained");
        check(gesture.move(15, 25), "returning to start still drag");
        check(gesture.release(15, 25) == DragGesture.Release.DRAG, "drag must not open menu");
        check(gesture.press(15, 25, bounds), "third press");
        check(gesture.release(99, 99) == DragGesture.Release.DRAG, "release includes last movement");
        check(gesture.press(15, 25, bounds), "fourth press");
        gesture.move(-500, 2000);
        check(gesture.release(-500, 2000) == DragGesture.Release.DRAG, "release outside board ends drag");
        gesture.press(15, 25, bounds);
        gesture.cancel();
        check(gesture.release(15, 25) == DragGesture.Release.NONE, "screen close clears capture");
        System.out.println("PASS: " + checks + " core checks (layout, resizing, drag/click separation)");
    }
}
