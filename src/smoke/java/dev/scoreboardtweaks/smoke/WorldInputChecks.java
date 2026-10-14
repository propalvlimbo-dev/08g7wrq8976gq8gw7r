package dev.scoreboardtweaks.smoke;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.core.Layout;
import dev.scoreboardtweaks.gui.SettingsScreen;
import dev.scoreboardtweaks.gui.SidebarRenderer;
import dev.scoreboardtweaks.mixin.ChatScreenAccessor;
import dev.scoreboardtweaks.smoke.mixin.MouseAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/** Real local world + vanilla Mouse callbacks, not direct calls to the gesture helper. */
public final class WorldInputChecks {
    private WorldInputChecks() {}
    public static void start(MinecraftClient client) {
        new Thread(() -> {
            try {
                System.out.println("WORLD_INPUT: creating a real offline singleplayer world");
                on(client, () -> { CreateWorldScreen.show(client, new TitleScreen()); return null; });
                waitFor(client, () -> client.currentScreen instanceof CreateWorldScreen);
                on(client, () -> {
                    ButtonWidget create = client.currentScreen.children().stream()
                        .filter(e -> e instanceof ButtonWidget)
                        .map(e -> (ButtonWidget) e)
                        .filter(b -> b.getMessage().getString().equals(Text.translatable("selectWorld.create").getString()))
                        .findFirst().orElseThrow(() -> new AssertionError("Create World button missing"));
                    create.onPress();
                    return null;
                });
                waitFor(client, () -> client.world != null && client.player != null && client.currentScreen == null);
                on(client, () -> {
                    ScoreboardTweaks.reset();
                    var board = client.world.getScoreboard();
                    var objective = board.addObjective("input_test", ScoreboardCriterion.DUMMY,
                        Text.literal("Input test"), ScoreboardCriterion.RenderType.INTEGER, false, null);
                    board.getOrCreateScore(ScoreHolder.fromName("Alpha"), objective).setScore(7);
                    board.getOrCreateScore(ScoreHolder.fromName("Beta"), objective).setScore(3);
                    board.getOrCreateScore(ScoreHolder.fromName("Gamma"), objective).setScore(0);
                    board.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
                    client.setScreen(new ChatScreen("unsent draft"));
                    return null;
                });
                Thread.sleep(500);
                for (int scale : new int[] {2, 3}) {
                    on(client, () -> { client.options.getGuiScale().setValue(scale); client.onResolutionChanged(); return null; });
                    Thread.sleep(500);
                    Layout.Bounds bounds = on(client, SidebarRenderer::bounds);
                    require(bounds != null, "sidebar has actual frame bounds at GUI scale " + scale);
                    double x = bounds.x() + 12, y = bounds.y() + 12;
                    cursor(client, x, y);
                    button(client, GLFW.GLFW_PRESS);
                    Thread.sleep(80);
                    button(client, GLFW.GLFW_RELEASE);
                    Thread.sleep(200);
                    on(client, () -> {
                        require(client.currentScreen instanceof SettingsScreen, "real Mouse click opens menu, scale=" + scale);
                        client.currentScreen.close();
                        require(client.currentScreen instanceof ChatScreen, "menu returns to chat");
                        require(((ChatScreenAccessor) client.currentScreen).scoreboardtweaks$getChatField().getText().equals("unsent draft"), "chat draft preserved");
                        return null;
                    });
                    Thread.sleep(300);
                    bounds = on(client, SidebarRenderer::bounds);
                    cursor(client, bounds.x() + 10, bounds.y() + 10);
                    button(client, GLFW.GLFW_PRESS);
                    Thread.sleep(80);
                    cursor(client, 40, 70);
                    Thread.sleep(300);
                    button(client, GLFW.GLFW_RELEASE);
                    Thread.sleep(250);
                    on(client, () -> {
                        require(client.currentScreen instanceof ChatScreen, "drag must not open menu");
                        require(ScoreboardTweaks.config().customPosition, "Mouse drag updates config, scale=" + scale);
                        Layout.Bounds moved = SidebarRenderer.bounds();
                        require(Math.abs(moved.x() - 30) <= 2 && Math.abs(moved.y() - 60) <= 2,
                            "real mouse drag reaches expected position, scale=" + scale + ", bounds=" + moved);
                        ScoreboardTweaks.config().resetPosition();
                        return null;
                    });
                    System.out.println("WORLD_INPUT_PASS: Mouse click + drag + draft, GUI scale " + scale);
                    Thread.sleep(300);
                }
                // Outside clicks still go to the chat field, not the settings menu.
                on(client, () -> {
                    require(!client.currentScreen.mouseClicked(3, 30, GLFW.GLFW_MOUSE_BUTTON_RIGHT), "right click not consumed by sidebar");
                    client.currentScreen.keyPressed(GLFW.GLFW_KEY_K, 0, GLFW.GLFW_MOD_CONTROL);
                    require(client.currentScreen instanceof SettingsScreen, "Ctrl+K fallback");
                    client.currentScreen.close();
                    ButtonWidget settings = client.currentScreen.children().stream()
                        .filter(e -> e instanceof ButtonWidget).map(e -> (ButtonWidget)e)
                        .filter(b -> b.getMessage().getString().equals(Text.translatable("scoreboardtweaks.open_settings").getString()))
                        .findFirst().orElseThrow(() -> new AssertionError("Visible settings button missing"));
                    client.currentScreen.mouseClicked(settings.getX() + 3, settings.getY() + 3, 0);
                    require(client.currentScreen instanceof SettingsScreen, "visible fallback button opens settings");
                    client.currentScreen.close();
                    // Continue with palette, scaling and click-to-place checks outside the render thread.
                    return null;
                });
                FeatureChecks.run(client);
                on(client, () -> {
                    System.out.println("SCOREBOARD_SMOKE_PASS: real-world Mouse click/drag, GUI scales 2/3, board scales 50/100/150/200, palette/HEX, size slider, click-to-place, save/reload");
                    client.scheduleStop();
                    return null;
                });
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        }, "scoreboard-world-input-checks").start();
    }

    static void cursor(MinecraftClient client, double x, double y) throws Exception {
        on(client, () -> {
            var window = client.getWindow();
            ((MouseAccessor) client.mouse).smoke$cursor(window.getHandle(),
                x * window.getWidth() / window.getScaledWidth(), y * window.getHeight() / window.getScaledHeight());
            return null;
        });
    }
    static void button(MinecraftClient client, int action) throws Exception {
        on(client, () -> { ((MouseAccessor)client.mouse).smoke$button(client.getWindow().getHandle(), 0, action, 0); return null; });
    }
    static <T> T on(MinecraftClient client, Callable<T> action) throws Exception {
        return client.submit(() -> {
            try { return action.call(); }
            catch (Exception e) { throw new RuntimeException(e); }
        }).get(90, TimeUnit.SECONDS);
    }
    private static void waitFor(MinecraftClient client, Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(160);
        while (System.nanoTime() < deadline) {
            if (on(client, condition)) return;
            Thread.sleep(500);
        }
        throw new AssertionError("Timed out waiting for game state");
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
