package dev.scoreboardtweaks.gui;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.core.ColorMath;
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
    private static final int SV_WIDTH = 230, SV_HEIGHT = 132;
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
        left = (width - 300) / 2;
        top = (height - 232) / 2;
        dragTarget = 0;
        hex = new TextFieldWidget(textRenderer, left + 50, top + 176, 110, 18, Text.translatable("scoreboardtweaks.hex"));
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
            }).dimensions(left + 172 + i * 29, top + 175, 26, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> {
            ScoreboardTweaks.config().backgroundRgb = original; close();
        }).dimensions(left + 12, top + 206, 134, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
            .dimensions(left + 154, top + 206, 134, 20).build());
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
        context.fill(left, top, left + 300, top + 232, 0xF51B202B);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 10, 0xFFFFFFFF);
        int px = left + 12, py = top + 32;
        // A small, bounded number of quads; no textures or native image allocations per frame.
        for (int y = 0; y < SV_HEIGHT; y += 3) {
            for (int x = 0; x < SV_WIDTH; x += 3) {
                int color = 0xFF000000 | ColorMath.rgb(hue, x / (double)(SV_WIDTH - 1), 1 - y / (double)(SV_HEIGHT - 1));
                context.fill(px + x, py + y, px + Math.min(x + 3, SV_WIDTH), py + Math.min(y + 3, SV_HEIGHT), color);
            }
        }
        for (int y = 0; y < SV_HEIGHT; y++)
            context.fill(left + 260, py + y, left + 282, py + y + 1, 0xFF000000 | ColorMath.rgb(y / (double)(SV_HEIGHT - 1), 1, 1));
        int sx = px + (int)Math.round(saturation * (SV_WIDTH - 1));
        int sy = py + (int)Math.round((1 - value) * (SV_HEIGHT - 1));
        context.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000);
        context.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xFFFFFFFF);
        context.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xFF000000 | ScoreboardTweaks.config().backgroundRgb);
        int hy = py + (int)Math.round(hue * (SV_HEIGHT - 1));
        context.fill(left + 257, hy - 2, left + 285, hy + 3, 0xFF000000);
        context.fill(left + 259, hy - 1, left + 283, hy + 2, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
        context.fill(left + 12, top + 176, left + 42, top + 194, 0xFF000000 | ScoreboardTweaks.config().backgroundRgb);
        int[] presets = {0x000000, 0xFFFFFF, 0x102C54, 0x382052};
        for (int i = 0; i < presets.length; i++) context.fill(left + 177 + i * 29, top + 180, left + 193 + i * 29, top + 190, 0xFF000000 | presets[i]);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            if (x >= left + 12 && x < left + 12 + SV_WIDTH && y >= top + 32 && y < top + 32 + SV_HEIGHT) dragTarget = 1;
            else if (x >= left + 256 && x < left + 286 && y >= top + 32 && y < top + 32 + SV_HEIGHT) dragTarget = 2;
            if (dragTarget != 0) { setFocused(null); updateFromMouse(x, y); return true; }
        }
        return super.mouseClicked(x, y, button);
    }
    private void updateFromMouse(double x, double y) {
        if (dragTarget == 1) {
            saturation = Layout.unit((x - left - 12) / (SV_WIDTH - 1));
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
