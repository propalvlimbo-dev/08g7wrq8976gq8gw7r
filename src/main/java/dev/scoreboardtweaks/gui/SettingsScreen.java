package dev.scoreboardtweaks.gui;

import dev.scoreboardtweaks.ScoreboardTweaks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Locale;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget hex;
    private int left, top;

    public SettingsScreen(Screen parent) {
        super(Text.translatable("scoreboardtweaks.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        left = (width - 300) / 2;
        top = (height - 224) / 2;
        addDrawableChild(ButtonWidget.builder(numbersLabel(), button -> {
            ScoreboardTweaks.config().hideNumbers = !ScoreboardTweaks.config().hideNumbers;
            button.setMessage(numbersLabel());
        }).dimensions(left + 12, top + 44, 276, 20).build());
        addDrawableChild(ButtonWidget.builder(backgroundLabel(), button -> {
            ScoreboardTweaks.config().backgroundEnabled = !ScoreboardTweaks.config().backgroundEnabled;
            button.setMessage(backgroundLabel());
        }).dimensions(left + 12, top + 68, 276, 20).build());

        hex = new TextFieldWidget(textRenderer, left + 146, top + 94, 142, 18,
            Text.translatable("scoreboardtweaks.hex"));
        hex.setMaxLength(7);
        hex.setText(String.format(Locale.ROOT, "#%06X", ScoreboardTweaks.config().backgroundRgb));
        hex.setChangedListener(value -> {
            boolean valid = value.matches("#?[0-9a-fA-F]{6}");
            hex.setEditableColor(valid ? 0xFFFFFF : 0xFF7777);
            if (valid) ScoreboardTweaks.config().backgroundRgb = Integer.parseInt(value.replace("#", ""), 16);
        });
        addDrawableChild(hex);
        addDrawableChild(new OpacitySlider(left + 12, top + 118));
        preset("scoreboardtweaks.black", "#000000", left + 12);
        preset("scoreboardtweaks.blue", "#102C54", left + 106);
        preset("scoreboardtweaks.purple", "#382052", left + 200);
        addDrawableChild(ButtonWidget.builder(Text.translatable("scoreboardtweaks.reset_position"), button -> {
            ScoreboardTweaks.config().resetPosition();
        }).dimensions(left + 12, top + 170, 136, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("scoreboardtweaks.reset_all"), button -> {
            ScoreboardTweaks.reset();
            clearAndInit();
        }).dimensions(left + 152, top + 170, 136, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
            .dimensions(left + 12, top + 198, 276, 20).build());
    }

    private void preset(String label, String color, int x) {
        addDrawableChild(ButtonWidget.builder(Text.translatable(label), button -> hex.setText(color))
            .dimensions(x, top + 144, 88, 20).build());
    }
    private Text numbersLabel() {
        return Text.translatable("scoreboardtweaks.numbers", Text.translatable(
            ScoreboardTweaks.config().hideNumbers ? "scoreboardtweaks.hidden" : "scoreboardtweaks.visible"));
    }
    private Text backgroundLabel() {
        return Text.translatable("scoreboardtweaks.background", Text.translatable(
            ScoreboardTweaks.config().backgroundEnabled ? "options.on" : "options.off"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // No full-screen blur: the real sidebar remains visible as a live preview.
        context.fill(left, top, left + 300, top + 224, 0xF51B202B);
        context.fill(left, top, left + 300, top + 2, 0xFF77BBFF);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top + 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("scoreboardtweaks.live_hint"),
            width / 2, top + 27, 0xFFADBACC);
        context.drawTextWithShadow(textRenderer, Text.translatable("scoreboardtweaks.hex"), left + 12, top + 99, 0xFFFFFFFF);
        context.fill(left + 126, top + 97, left + 138, top + 109, 0xFF000000 | ScoreboardTweaks.config().backgroundRgb);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Keep the world and sidebar unobscured while editing.
    }
    @Override
    public boolean shouldPause() { return false; }
    @Override
    public void close() { if (client != null) client.setScreen(parent); }
    @Override
    public void removed() { ScoreboardTweaks.save(); }

    private static final class OpacitySlider extends SliderWidget {
        OpacitySlider(int x, int y) {
            super(x, y, 276, 20, Text.empty(), ScoreboardTweaks.config().backgroundOpacity / 255.0);
            updateMessage();
        }
        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("scoreboardtweaks.opacity", Math.round(value * 100)));
        }
        @Override
        protected void applyValue() {
            ScoreboardTweaks.config().backgroundOpacity = (int) Math.round(value * 255);
        }
    }
}
