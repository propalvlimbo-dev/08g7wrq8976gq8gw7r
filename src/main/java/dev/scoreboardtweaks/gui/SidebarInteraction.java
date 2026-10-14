package dev.scoreboardtweaks.gui;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.core.DragGesture;
import dev.scoreboardtweaks.core.Layout;
import dev.scoreboardtweaks.mixin.ChatScreenAccessor;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

public final class SidebarInteraction {
    private static final DragGesture GESTURE = new DragGesture();
    private SidebarInteraction() {}

    public static void checkScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof ChatScreen) || client.world == null || client.options.hudHidden) {
            if (GESTURE.isDragging()) ScoreboardTweaks.save();
            GESTURE.cancel();
        }
    }

    /** Coordinates come from ChatScreen, already converted to GUI pixels by Minecraft. */
    public static boolean mousePressed(double x, double y, int button) {
        checkScreen();
        if (!canInteract() || button != 0) return false;
        return GESTURE.press(x, y, SidebarRenderer.bounds(), 6);
    }

    public static boolean mouseReleased(double x, double y, int button) {
        checkScreen();
        if (!canInteract() || button != 0 || !GESTURE.isPressed()) return false;
        move(x, y);
        DragGesture.Release released = GESTURE.release(x, y);
        if (released == DragGesture.Release.DRAG) ScoreboardTweaks.save();
        else if (released == DragGesture.Release.CLICK) openSettings();
        return true;
    }

    public static boolean mouseDragged(double x, double y, int button) {
        checkScreen();
        if (!canInteract() || button != 0 || !GESTURE.isPressed()) return false;
        move(x, y);
        return true;
    }

    private static boolean canInteract() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.currentScreen instanceof ChatScreen && client.world != null && !client.options.hudHidden;
    }

    private static void move(double x, double y) {
        if (!GESTURE.move(x, y)) return;
        Layout.Bounds bounds = SidebarRenderer.bounds();
        if (bounds == null) return;
        var window = MinecraftClient.getInstance().getWindow();
        var config = ScoreboardTweaks.config();
        config.customPosition = true;
        config.anchorX = Layout.toAnchor(GESTURE.left(x), bounds.width(), window.getScaledWidth());
        config.anchorY = Layout.toAnchor(GESTURE.top(y), bounds.height(), window.getScaledHeight());
    }

    public static void openSettings() {
        MinecraftClient.getInstance().setScreen(new SettingsScreen(parentWithDraft()));
    }

    private static Screen parentWithDraft() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (GESTURE.isDragging()) ScoreboardTweaks.save();
        GESTURE.cancel();
        Screen parent = client.currentScreen;
        if (parent instanceof ChatScreen) {
            String draft = ((ChatScreenAccessor) parent).scoreboardtweaks$getChatField().getText();
            parent = new ChatScreen(draft);
        }
        return parent;
    }

}
