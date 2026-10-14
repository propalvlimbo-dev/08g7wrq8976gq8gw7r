package dev.scoreboardtweaks.core;

/** Non-overlapping controls and live preview, including the minimum 320x240 GUI. */
public record EditorLayout(Layout.Bounds panel, Layout.Bounds preview) {
    public static EditorLayout of(int width, int height) {
        int panelWidth = width >= 500 ? 300 : 208;
        int previewWidth = Math.min(200, Math.max(64, width - panelWidth - 24));
        int left = (width - panelWidth - 8 - previewWidth) / 2;
        int top = (height - 232) / 2;
        return new EditorLayout(new Layout.Bounds(left, top, panelWidth, 232),
            new Layout.Bounds(left + panelWidth + 8, top, previewWidth, 232));
    }

    public Layout.Bounds previewContent() {
        return new Layout.Bounds(preview.x() + 8, preview.y() + 30, preview.width() - 16, preview.height() - 54);
    }
}
