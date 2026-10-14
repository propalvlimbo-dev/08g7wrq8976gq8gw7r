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
        check(Layout.scale(Double.NaN) == 1, "invalid scale defaults to 100 percent");
        check(Layout.scale(0.1) == 0.5 && Layout.scale(9) == 2, "scale range");
        check(Layout.fitScale(2, 400, 100, 320, 240) == 0.8, "large board fits screen");
        check(Layout.fitScale(2, 100, 400, 320, 240) == 0.6, "tall board fits screen");
        check(bounds.expanded(6).contains(5, 25), "expanded target includes near-edge point");
        check(!bounds.expanded(6).contains(3, 25), "expanded target has bounded margin");
        gesture.cancel();
        check(gesture.press(5, 25, bounds, 6), "near-border press captures");
        check(gesture.left(25) == 30, "padding keeps original grab offset without jumping");
        gesture.cancel();
        check(ColorMath.rgb(0, 1, 1) == 0xFF0000, "HSV red");
        check(ColorMath.rgb(1.0 / 3, 1, 1) == 0x00FF00, "HSV green");
        check(ColorMath.rgb(2.0 / 3, 1, 1) == 0x0000FF, "HSV blue");
        check(ColorMath.rgb(0.8, 0, 1) == 0xFFFFFF, "HSV white");
        check(ColorMath.rgb(0.5, 1, 0) == 0, "HSV black");
        check(ColorMath.rgb(1, 1, 1) == 0xFF0000, "hue wraps");
        for (int rgb : new int[] {0, 0xFFFFFF, 0x102C54, 0x382052, 0xFF0000, 0xABCDEF, 0x808080, 0x010203}) {
            double[] hsv = ColorMath.hsv(rgb);
            check(ColorMath.rgb(hsv[0], hsv[1], hsv[2]) == rgb, "HSV hex round trip " + rgb);
        }
        for (int w : new int[] {320, 427, 480, 500, 640, 960}) {
            for (int h : new int[] {240, 360, 540}) {
                EditorLayout editor = EditorLayout.of(w, h);
                var panel = editor.panel(); var preview = editor.preview(); var content = editor.previewContent();
                check(panel.x() >= 0 && panel.y() >= 0, "controls inside screen");
                check(panel.x() + panel.width() < preview.x(), "preview cannot be covered by controls");
                check(preview.x() + preview.width() <= w && preview.y() + preview.height() <= h, "preview inside screen");
                check(content.width() > 0 && content.height() > 0 && preview.contains(content.x(), content.y()), "preview content valid");
            }
        }
        System.out.println("PASS: " + checks + " core checks (layout, resizing, drag/click separation)");
    }
}
