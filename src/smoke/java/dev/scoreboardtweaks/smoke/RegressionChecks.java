package dev.scoreboardtweaks.smoke;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.core.EditorLayout;
import dev.scoreboardtweaks.gui.ColorPickerScreen;
import dev.scoreboardtweaks.gui.SettingsScreen;
import dev.scoreboardtweaks.gui.SidebarInteraction;
import dev.scoreboardtweaks.gui.SidebarRenderer;
import dev.scoreboardtweaks.mixin.ChatScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import static dev.scoreboardtweaks.smoke.WorldInputChecks.on;

/** Regression checks for the reported keyboard focus and live rendering bugs. */
public final class RegressionChecks {
    public static void run(MinecraftClient client) throws Exception {
        on(client, () -> {
            client.inGameHud.getChatHud().addToMessageHistory("history-older");
            client.inGameHud.getChatHud().addToMessageHistory("history-latest");
            client.setScreen(new ChatScreen("draft-to-restore"));
            var chat = client.currentScreen;
            require(chat.children().stream().noneMatch(e -> e instanceof ButtonWidget), "chat toolbar must not participate in keyboard focus");
            var field = ((ChatScreenAccessor) chat).scoreboardtweaks$getChatField();
            for (int cycle = 0; cycle < 4; cycle++) {
                chat.keyPressed(GLFW.GLFW_KEY_UP, 0, 0);
                require(field.getText().equals("history-latest"), "Up selects latest message, not a button");
                chat.keyPressed(GLFW.GLFW_KEY_UP, 0, 0);
                require(field.getText().equals("history-older"), "second Up selects previous message");
                chat.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
                require(field.getText().equals("history-latest"), "Down selects newer message");
                chat.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
                require(field.getText().equals("draft-to-restore"), "Down restores unsent draft");
                require(!(chat.getFocused() instanceof ButtonWidget), "arrows never focus the overlay");
            }
            client.currentScreen.mouseClicked(chat.width - 100, 10, 0);
            require(client.currentScreen instanceof SettingsScreen, "mouse-only button still opens settings");
            require(client.currentScreen.children().stream().noneMatch(e -> e instanceof ButtonWidget b && b.getMessage().getString().contains("Переместить")), "Move button removed");
            var config = ScoreboardTweaks.config();
            config.scale = 1; config.backgroundEnabled = true; config.backgroundOpacity = 255;
            setHex(client, "#D02040");
            return null;
        });
        Thread.sleep(180);
        assertPixel(client, 0xD02040);
        for (int guiScale : new int[] {2, 3, 4}) {
            on(client, () -> {
                client.options.getGuiScale().setValue(guiScale); client.onResolutionChanged();
                setHex(client, "#20D040");
                return null;
            });
            Thread.sleep(180);
            assertPixel(client, 0x20D040);
            on(client, () -> { setHex(client, "#2040D0"); return null; });
            Thread.sleep(180);
            assertPixel(client, 0x2040D0);
            for (int cycle = 0; cycle < 3; cycle++) {
                on(client, () -> { pressColor(client); client.options.hudHidden = true; return null; });
                Thread.sleep(150);
                assertPixel(client, 0x2040D0); // Preview remains visible even when the HUD is hidden.
                on(client, () -> { client.currentScreen.close(); return null; });
                Thread.sleep(150);
                assertPixel(client, 0x2040D0);
            }
            on(client, () -> { client.options.hudHidden = false; return null; });
            System.out.println("PREVIEW_PIXEL_PASS: live HEX/background at GUI scale " + guiScale);
        }
        on(client, () -> { ScoreboardTweaks.config().backgroundOpacity = 0; return null; });
        Thread.sleep(150);
        on(client, () -> {
            int rgb = samplePreview(client);
            require(rgb == 0x343943 || rgb == 0x414752, "transparent background reveals checkerboard immediately, got " + Integer.toHexString(rgb));
            ScoreboardTweaks.config().backgroundOpacity = 96;
            client.currentScreen.close();
            require(client.currentScreen instanceof ChatScreen, "settings returns to chat");
            require(((ChatScreenAccessor)client.currentScreen).scoreboardtweaks$getChatField().getText().equals("draft-to-restore"), "history draft preserved through settings");
            System.out.println("REGRESSION_PASS: Up/Down history, mouse-only toolbar, absent Move button, stable preview, pixel-verified live changes including F1/resize");
            return null;
        });
    }
    private static void setHex(MinecraftClient client, String value) {
        var field = client.currentScreen.children().stream().filter(e -> e instanceof TextFieldWidget)
            .map(e -> (TextFieldWidget)e).findFirst().orElseThrow();
        field.setText(value);
    }
    private static void pressColor(MinecraftClient client) {
        ButtonWidget color = client.currentScreen.children().stream().filter(e -> e instanceof ButtonWidget)
            .map(e -> (ButtonWidget)e).filter(b -> b.getMessage().getString().equals(Text.translatable("scoreboardtweaks.color").getString()))
            .findFirst().orElseThrow();
        client.currentScreen.mouseClicked(color.getX() + 5, color.getY() + 5, 0);
        require(client.currentScreen instanceof ColorPickerScreen, "palette opens");
    }
    private static void assertPixel(MinecraftClient client, int expected) throws Exception {
        on(client, () -> {
            int actual = samplePreview(client);
            require(actual == expected, "visible preview pixel should update to " + Integer.toHexString(expected) + ", got " + Integer.toHexString(actual));
            return null;
        });
    }
    private static int samplePreview(MinecraftClient client) {
        var b = SidebarRenderer.previewBounds();
        require(b != null, "preview must be rendered on settings/palette every frame");
        var layout = EditorLayout.of(client.currentScreen.width, client.currentScreen.height);
        require(b.x() >= layout.previewContent().x() && b.x() + b.width() <= layout.previewContent().x() + layout.previewContent().width(), "preview stays separate from controls");
        double scale = client.getWindow().getScaleFactor();
        try (var image = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            // The top row of the sidebar is background only; avoid glyphs and edges.
            int x = (int)Math.floor((b.x() + b.width() / 2.0) * scale);
            int y = (int)Math.floor((b.y() + 1) * scale);
            return image.getColorArgb(x, y) & 0xFFFFFF;
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
