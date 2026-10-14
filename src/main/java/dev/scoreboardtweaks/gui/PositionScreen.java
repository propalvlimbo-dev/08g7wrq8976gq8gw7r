package dev.scoreboardtweaks.gui;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.core.DragGesture;
import dev.scoreboardtweaks.core.Layout;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Explicit placement mode: the user can click a destination without hitting the old panel. */
public final class PositionScreen extends Screen {
    private final Screen parent;
    private final DragGesture gesture = new DragGesture();
    public PositionScreen(Screen parent) {
        super(Text.translatable("scoreboardtweaks.move"));
        this.parent = parent;
    }
    @Override protected void init() {
        gesture.cancel();
        addDrawableChild(ButtonWidget.builder(Text.translatable("scoreboardtweaks.reset_position"), b -> {
            ScoreboardTweaks.config().resetPosition();
        }).dimensions(width / 2 - 148, height - 26, 144, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
            .dimensions(width / 2 + 4, height - 26, 144, 20).build());
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean available = SidebarRenderer.renderActive(context);
        context.fill(0, 0, width, 24, 0xC018202B);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("scoreboardtweaks.place_hint"), width / 2, 8, 0xFFFFFFFF);
        if (available) {
            var b = SidebarRenderer.bounds().expanded(6);
            int color = gesture.isPressed() ? 0xFF55EFC4 : 0xFF77BBFF;
            context.fill(b.x(), b.y(), b.x() + b.width(), b.y() + 2, color);
            context.fill(b.x(), b.y() + b.height() - 2, b.x() + b.width(), b.y() + b.height(), color);
            context.fill(b.x(), b.y(), b.x() + 2, b.y() + b.height(), color);
            context.fill(b.x() + b.width() - 2, b.y(), b.x() + b.width(), b.y() + b.height(), color);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("scoreboardtweaks.no_sidebar"), width / 2, height / 2, 0xFFFFCC77);
        }
        super.render(context, mouseX, mouseY, delta);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        var bounds = SidebarRenderer.bounds();
        if (button != 0 || bounds == null) return false;
        if (!bounds.expanded(6).contains(x, y)) {
            // Click-to-place is the fallback for a panel that is hard to target.
            place(x - bounds.width() / 2.0, y - bounds.height() / 2.0, bounds);
            var config = ScoreboardTweaks.config();
            bounds = new Layout.Bounds(Layout.fromAnchor(config.anchorX, bounds.width(), width),
                Layout.fromAnchor(config.anchorY, bounds.height(), height), bounds.width(), bounds.height());
        }
        gesture.press(x, y, bounds, 6);
        setDragging(true);
        return true;
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && gesture.isPressed()) {
            if (gesture.move(x, y) && SidebarRenderer.bounds() != null)
                place(gesture.left(x), gesture.top(y), SidebarRenderer.bounds());
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && gesture.isPressed()) {
            mouseDragged(x, y, button, 0, 0);
            gesture.release(x, y);
            setDragging(false);
            ScoreboardTweaks.save();
            return true;
        }
        return super.mouseReleased(x, y, button);
    }
    private void place(double x, double y, Layout.Bounds b) {
        var config = ScoreboardTweaks.config();
        config.customPosition = true;
        config.anchorX = Layout.toAnchor(x, b.width(), width);
        config.anchorY = Layout.toAnchor(y, b.height(), height);
    }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
    @Override public boolean shouldPause() { return false; }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public void removed() { gesture.cancel(); ScoreboardTweaks.save(); }
}
