package dev.scoreboardtweaks.gui;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.core.ColorMath;
import dev.scoreboardtweaks.core.EditorLayout;
import dev.scoreboardtweaks.core.Layout;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.Locale;

public final class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final int original;
    private double hue, saturation, value;
    private int left, top, dragTarget;
    private TextFieldWidget hex;
    private boolean updatingHex;
    private static final int SV_HEIGHT = 132;
    private EditorLayout layout;
    private int panelWidth, svWidth, hueLeft;
    public ColorPickerScreen(Screen parent) {
        super(Text.translatable("scoreboardtweaks.palette"));
        this.parent = parent;
        original = ScoreboardTweaks.config().backgroundRgb;
        loadColor(original);
    }
    private void loadColor(int rgb) {
        double[] hsv = ColorMath.hsv(rgb);
        hue = hsv[0]; saturation = hsv[1]; value = hsv[2];
    }
    @Override protected void init() {
        layout = EditorLayout.of(width, height);
        left = layout.panel().x(); top = layout.panel().y(); panelWidth = layout.panel().width();
        svWidth = panelWidth - 70;
        hueLeft = left + panelWidth - 40;
        dragTarget = 0;
        hex = new TextFieldWidget(textRenderer, left + 42, top + 176, panelWidth - 134, 18, Text.translatable("scoreboardtweaks.hex"));
        hex.setMaxLength(7);
        hex.setText(String.format(Locale.ROOT, "#%06X", ScoreboardTweaks.config().backgroundRgb));
        hex.setChangedListener(text -> {
            if (updatingHex) return;
            boolean valid = text.matches("#?[0-9a-fA-F]{6}");
            hex.setEditableColor(valid ? 0xFFFFFF : 0xFF7777);
            if (valid) {
                int rgb = Integer.parseInt(text.replace("#", ""), 16);
                loadColor(rgb);
                ScoreboardTweaks.config().backgroundRgb = rgb;
            }
        });
        addDrawableChild(hex);
        int[] presets = {0x000000, 0xFFFFFF, 0x102C54, 0x382052};
        for (int i = 0; i < presets.length; i++) {
            final int rgb = presets[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(""), b -> {
                loadColor(rgb); applyColor();
            }).dimensions(left + panelWidth - 84 + i * 19, top + 175, 18, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> {
            ScoreboardTweaks.config().backgroundRgb = original; close();
        }).dimensions(left + 12, top + 206, (panelWidth - 32) / 2, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
            .dimensions(left + panelWidth / 2 + 4, top + 206, (panelWidth - 32) / 2, 20).build());
    }
    private void applyColor() {
        int rgb = ColorMath.rgb(hue, saturation, value);
        ScoreboardTweaks.config().backgroundRgb = rgb;
        updatingHex = true;
        hex.setText(String.format(Locale.ROOT, "#%06X", rgb));
        hex.setEditableColor(0xFFFFFF);
        updatingHex = false;
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(left, top, left + panelWidth, top + 232, 0xF51B202B);
        context.drawCenteredTextWithShadow(textRenderer, title, left + panelWidth / 2, top + 10, 0xFFFFFFFF);
        int px = left + 12, py = top + 32;
        // HSV at fixed saturation is a linear vertical gradient. A column gradient
        // replaces dozens of cell quads: ~120 quads instead of ~3500 each frame.
        for (int x = 0; x < svWidth; x += 2) {
            int bright = 0xFF000000 | ColorMath.rgb(hue, x / (double)(svWidth - 1), 1);
            context.fillGradient(px + x, py, px + Math.min(x + 2, svWidth), py + SV_HEIGHT, bright, 0xFF000000);
        }
        for (int sector = 0; sector < 6; sector++) {
            context.fillGradient(hueLeft, py + sector * SV_HEIGHT / 6, hueLeft + 22, py + (sector + 1) * SV_HEIGHT / 6,
                0xFF000000 | ColorMath.rgb(sector / 6.0, 1, 1), 0xFF000000 | ColorMath.rgb((sector + 1) / 6.0, 1, 1));
        }
        int sx = px + (int)Math.round(saturation * (svWidth - 1));
        int sy = py + (int)Math.round((1 - value) * (SV_HEIGHT - 1));
        context.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000);
        context.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFFFFFF);
        context.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xFF000000 | ScoreboardTweaks.config().backgroundRgb);
        int hy = py + (int)Math.round(hue * (SV_HEIGHT - 1));
        context.fill(hueLeft - 3, hy - 2, hueLeft + 25, hy + 3, 0xFF000000);
        context.fill(hueLeft - 1, hy - 1, hueLeft + 23, hy + 2, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
        context.fill(left + 12, top + 176, left + 36, top + 194, 0xFF000000 | ScoreboardTweaks.config().backgroundRgb);
        int[] presets = {0x000000, 0xFFFFFF, 0x102C54, 0x382052};
        for (int i = 0; i < presets.length; i++) context.fill(left + panelWidth - 81 + i * 19, top + 180, left + panelWidth - 69 + i * 19, top + 190, 0xFF000000 | presets[i]);
        SidebarRenderer.renderPreview(context, layout);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            if (x >= left + 12 && x < left + 12 + svWidth && y >= top + 32 && y < top + 32 + SV_HEIGHT) dragTarget = 1;
            else if (x >= hueLeft - 4 && x < hueLeft + 26 && y >= top + 32 && y < top + 32 + SV_HEIGHT) dragTarget = 2;
            if (dragTarget != 0) { setFocused(null); updateFromMouse(x, y); return true; }
        }
        return super.mouseClicked(x, y, button);
    }
    private void updateFromMouse(double x, double y) {
        if (dragTarget == 1) {
            saturation = Layout.unit((x - left - 12) / (svWidth - 1));
            value = 1 - Layout.unit((y - top - 32) / (SV_HEIGHT - 1));
        } else hue = Layout.unit((y - top - 32) / (SV_HEIGHT - 1));
        applyColor();
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && dragTarget != 0) { updateFromMouse(x, y); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && dragTarget != 0) { updateFromMouse(x, y); dragTarget = 0; return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    @Override public boolean shouldPause() { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public void removed() { dragTarget = 0; ScoreboardTweaks.save(); }
}
