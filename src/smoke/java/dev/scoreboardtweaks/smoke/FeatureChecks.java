package dev.scoreboardtweaks.smoke;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.config.ModConfig;
import dev.scoreboardtweaks.core.ColorMath;
import dev.scoreboardtweaks.core.Layout;
import dev.scoreboardtweaks.core.EditorLayout;
import dev.scoreboardtweaks.gui.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import static dev.scoreboardtweaks.smoke.WorldInputChecks.*;

public final class FeatureChecks {
    public static void run(MinecraftClient client) throws Exception {
        int previousWidth = 0;
        for (double scale : new double[] {0.5, 1, 1.5, 2}) {
            on(client, () -> {
                var config = ScoreboardTweaks.config();
                config.scale = scale;
                config.resetPosition();
                client.setScreen(new ChatScreen("feature draft"));
                return null;
            });
            Thread.sleep(250);
            var bounds = on(client, SidebarRenderer::bounds);
            require(bounds != null && bounds.width() > previousWidth, "size changes rendered width: " + scale);
            previousWidth = bounds.width();
            // Exercise the padded target, OUTSIDE the actual text/background rectangle.
            cursor(client, bounds.x() - 4, bounds.y() + 12);
            button(client, GLFW.GLFW_PRESS);
            Thread.sleep(80);
            button(client, GLFW.GLFW_RELEASE);
            on(client, () -> {
                require(client.currentScreen instanceof SettingsScreen, "padded target click at scale " + scale);
                client.currentScreen.close();
                return null;
            });
            Thread.sleep(200);
            bounds = on(client, SidebarRenderer::bounds);
            cursor(client, bounds.x() + 15, bounds.y() + 15);
            button(client, GLFW.GLFW_PRESS);
            Thread.sleep(80);
            cursor(client, 45, 75);
            Thread.sleep(200);
            button(client, GLFW.GLFW_RELEASE);
            Thread.sleep(200);
            on(client, () -> {
                var moved = SidebarRenderer.bounds();
                require(client.currentScreen instanceof ChatScreen, "scaled drag does not open menu");
                require(Math.abs(moved.x() - 30) <= 2 && Math.abs(moved.y() - 60) <= 2, "scaled target matches rendering " + scale + ": " + moved);
                return null;
            });
            System.out.println("SCALE_INPUT_PASS: " + scale);
        }
        on(client, () -> { SidebarInteraction.openSettings(); pressButton(client, "scoreboardtweaks.color"); return null; });
        Thread.sleep(250); // Exercise real palette rendering, not just its math.
        on(client, () -> {
            require(client.currentScreen instanceof ColorPickerScreen, "color sample opens palette");
            var panel = EditorLayout.of(client.currentScreen.width, client.currentScreen.height).panel();
            int left = panel.x(), top = panel.y(), svWidth = panel.width() - 70, hueLeft = left + panel.width() - 40;
            var screen = client.currentScreen;
            screen.mouseClicked(hueLeft + 10, top + 32 + 0.5 * 131, 0);
            screen.mouseReleased(hueLeft + 10, top + 32 + 0.5 * 131, 0);
            screen.mouseClicked(left + 12, top + 32, 0);
            screen.mouseDragged(left + 12 + 0.75 * (svWidth - 1), top + 32 + 0.2 * 131, 0, 0, 0);
            screen.mouseReleased(left + 12 + 0.75 * (svWidth - 1), top + 32 + 0.2 * 131, 0);
            require(ScoreboardTweaks.config().backgroundRgb == ColorMath.rgb(0.5, 0.75, 0.8), "HSV pick matches preview/HEX");
            screen.close();
            pressButton(client, "scoreboardtweaks.color");
            client.currentScreen.mouseClicked(left + 12, top + 32, 0);
            client.currentScreen.mouseReleased(left + 12, top + 32, 0);
            pressButton(client, "gui.cancel");
            require(ScoreboardTweaks.config().backgroundRgb == ColorMath.rgb(0.5, 0.75, 0.8), "cancel restores previous color");
            SliderWidget slider = client.currentScreen.children().stream().filter(e -> e instanceof SliderWidget)
                .map(e -> (SliderWidget)e).filter(e -> e.getMessage().getString().equals(Text.translatable("scoreboardtweaks.scale", 200).getString()))
                .findFirst().orElseThrow(() -> new AssertionError("Size slider missing"));
            double sx = slider.getX() + 4 + (slider.getWidth() - 8) / 3.0;
            client.currentScreen.mouseClicked(sx, slider.getY() + 10, 0);
            client.currentScreen.mouseReleased(sx, slider.getY() + 10, 0);
            require(Math.abs(ScoreboardTweaks.config().scale - 1) < 0.02, "size slider updates scale");
            var path = Files.createTempDirectory("scoreboard-features").resolve("config.json");
            require(ScoreboardTweaks.config().save(path), "save features");
            var loaded = ModConfig.load(path);
            require(loaded.scale == ScoreboardTweaks.config().scale && loaded.backgroundRgb == ScoreboardTweaks.config().backgroundRgb
                && loaded.customPosition, "color, scale, position persist together");
            System.out.println("FEATURE_PASS: palette apply/cancel, size slider, save/reload");
            return null;
        });
    }
    private static void pressButton(MinecraftClient client, String key) {
        ButtonWidget button = client.currentScreen.children().stream().filter(e -> e instanceof ButtonWidget)
            .map(e -> (ButtonWidget)e).filter(e -> e.getMessage().getString().equals(Text.translatable(key).getString()))
            .findFirst().orElseThrow(() -> new AssertionError("Button missing: " + key));
        client.currentScreen.mouseClicked(button.getX() + 5, button.getY() + 5, 0);
        client.currentScreen.mouseReleased(button.getX() + 5, button.getY() + 5, 0);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
