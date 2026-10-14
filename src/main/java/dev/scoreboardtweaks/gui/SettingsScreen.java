package dev.scoreboardtweaks.gui;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.core.EditorLayout;
import dev.scoreboardtweaks.core.Layout;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.Locale;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private EditorLayout layout;
    private int left, top, panelWidth;
    public SettingsScreen(Screen parent) {
        super(Text.translatable("scoreboardtweaks.title"));
        this.parent = parent;
    }
    @Override protected void init() {
        layout = EditorLayout.of(width, height);
        left = layout.panel().x(); top = layout.panel().y(); panelWidth = layout.panel().width();
        int controlWidth = panelWidth - 24, half = (controlWidth - 4) / 2;
        addDrawableChild(ButtonWidget.builder(numbersLabel(), b -> {
            ScoreboardTweaks.config().hideNumbers = !ScoreboardTweaks.config().hideNumbers;
            b.setMessage(numbersLabel());
        }).dimensions(left + 12, top + 32, half, 20).build());
        addDrawableChild(ButtonWidget.builder(backgroundLabel(), b -> {
            ScoreboardTweaks.config().backgroundEnabled = !ScoreboardTweaks.config().backgroundEnabled;
            b.setMessage(backgroundLabel());
        }).dimensions(left + 16 + half, top + 32, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("scoreboardtweaks.color"), b ->
            client.setScreen(new ColorPickerScreen(this))).dimensions(left + 12, top + 58, half, 20).build());
        TextFieldWidget hex = new TextFieldWidget(textRenderer, left + 16 + half, top + 59, half, 18,
            Text.translatable("scoreboardtweaks.hex"));
        hex.setMaxLength(7);
        hex.setText(String.format(Locale.ROOT, "#%06X", ScoreboardTweaks.config().backgroundRgb));
        hex.setChangedListener(value -> {
            boolean valid = value.matches("#?[0-9a-fA-F]{6}");
            hex.setEditableColor(valid ? 0xFFFFFF : 0xFF7777);
            if (valid) ScoreboardTweaks.config().backgroundRgb = Integer.parseInt(value.replace("#", ""), 16);
        });
        addDrawableChild(hex);
        addDrawableChild(new OpacitySlider(left + 12, top + 84, controlWidth));
        addDrawableChild(new ScaleSlider(left + 12, top + 110, controlWidth));
        addDrawableChild(ButtonWidget.builder(Text.translatable("scoreboardtweaks.reset_position"), b ->
            ScoreboardTweaks.config().resetPosition()).dimensions(left + 12, top + 140, controlWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("scoreboardtweaks.reset_all"), b -> {
            ScoreboardTweaks.reset(); clearAndInit();
        }).dimensions(left + 12, top + 164, controlWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
            .dimensions(left + 12, top + 206, controlWidth, 20).build());
    }
    private Text numbersLabel() {
        return Text.translatable("scoreboardtweaks.numbers_short", Text.translatable(
            ScoreboardTweaks.config().hideNumbers ? "scoreboardtweaks.hidden" : "scoreboardtweaks.visible"));
    }
    private Text backgroundLabel() {
        return Text.translatable("scoreboardtweaks.background_short", Text.translatable(
            ScoreboardTweaks.config().backgroundEnabled ? "options.on" : "options.off"));
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(left, top, left + panelWidth, top + 232, 0xF51B202B);
        context.drawCenteredTextWithShadow(textRenderer, title, left + panelWidth / 2, top + 11, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("scoreboardtweaks.live_hint"), left + panelWidth / 2, top + 192, 0xFFADBACC);
        super.render(context, mouseX, mouseY, delta);
        int half = (panelWidth - 28) / 2;
        context.fill(left + half - 5, top + 62, left + half + 7, top + 74, 0xFF000000 | ScoreboardTweaks.config().backgroundRgb);
        // Separate preview column; never covered by controls or the vanilla HUD layers.
        SidebarRenderer.renderPreview(context, layout);
    }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    @Override public boolean shouldPause() { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public void removed() { ScoreboardTweaks.save(); }

    private static final class OpacitySlider extends SliderWidget {
        OpacitySlider(int x, int y, int width) {
            super(x, y, width, 20, Text.empty(), ScoreboardTweaks.config().backgroundOpacity / 255.0);
            updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Text.translatable("scoreboardtweaks.opacity", Math.round(value * 100)));
        }
        @Override protected void applyValue() { ScoreboardTweaks.config().backgroundOpacity = (int)Math.round(value * 255); }
    }
    private static final class ScaleSlider extends SliderWidget {
        ScaleSlider(int x, int y, int width) {
            super(x, y, width, 20, Text.empty(), (Layout.scale(ScoreboardTweaks.config().scale) - 0.5) / 1.5);
            updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Text.translatable("scoreboardtweaks.scale", Math.round((0.5 + value * 1.5) * 100)));
        }
        @Override protected void applyValue() {
            ScoreboardTweaks.config().scale = Math.round((0.5 + value * 1.5) * 100) / 100.0;
        }
    }
}
