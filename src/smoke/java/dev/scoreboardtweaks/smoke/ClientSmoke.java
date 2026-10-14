package dev.scoreboardtweaks.smoke;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.smoke.mixin.HudAccessor;
import dev.scoreboardtweaks.config.ModConfig;
import dev.scoreboardtweaks.gui.SettingsScreen;
import dev.scoreboardtweaks.gui.SidebarRenderer;
import dev.scoreboardtweaks.mixin.ChatScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;

/** No accounts or multiplayer servers are used. Runs only via runSmokeClient. */
public final class ClientSmoke implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("scoreboardtweaks.smoke")) return;
        Thread monitor = new Thread(() -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                // Queue all game access on the render thread; title screen signals resources are ready.
                for (int i = 0; i < 120; i++) {
                    Thread.sleep(1000);
                    boolean ready = client.submit(() -> client.currentScreen instanceof TitleScreen && client.getOverlay() == null).get();
                    if (ready) {
                        client.execute(() -> client.setScreen(new RenderChecks()));
                        return;
                    }
                }
                throw new AssertionError("Client never reached title screen");
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        }, "scoreboard-smoke-start");
        monitor.setDaemon(true);
        monitor.start();
    }

    private static final class RenderChecks extends Screen {
        private final Scoreboard scoreboard = new Scoreboard();
        private final ScoreboardObjective objective;
        private int frames;
        RenderChecks() {
            super(Text.literal("Sidebar smoke checks"));
            objective = scoreboard.addObjective("smoke", ScoreboardCriterion.DUMMY, Text.literal("Test"),
                ScoreboardCriterion.RenderType.INTEGER, false, null);
            scoreboard.getOrCreateScore(ScoreHolder.fromName("Alpha"), objective).setScore(12345);
            scoreboard.getOrCreateScore(ScoreHolder.fromName("Beta"), objective).setScore(3);
            scoreboard.getOrCreateScore(ScoreHolder.fromName("Gamma"), objective).setScore(0);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            try {
                var config = ScoreboardTweaks.config();
                config.hideNumbers = false;
                ((HudAccessor) client.inGameHud).smoke$renderSidebar(context, objective);
                int withNumbers = SidebarRenderer.bounds().width();
                config.hideNumbers = true;
                ((HudAccessor) client.inGameHud).smoke$renderSidebar(context, objective);
                require(SidebarRenderer.bounds().width() < withNumbers, "hidden numbers must reclaim width");
                config.customPosition = true;
                config.anchorX = 0;
                config.anchorY = 0;
                ((HudAccessor) client.inGameHud).smoke$renderSidebar(context, objective);
                require(SidebarRenderer.bounds().x() == 0 && SidebarRenderer.bounds().y() == 0, "top left position");
                config.anchorX = 1;
                config.anchorY = 1;
                ((HudAccessor) client.inGameHud).smoke$renderSidebar(context, objective);
                var b = SidebarRenderer.bounds();
                require(b.x() + b.width() == width && b.y() + b.height() == height, "bottom right position");
                config.backgroundEnabled = false;
                ((HudAccessor) client.inGameHud).smoke$renderSidebar(context, objective);
                if (++frames == 10) {
                    Path path = Files.createTempDirectory("scoreboard-smoke").resolve("test.json");
                    require(config.save(path), "config saves");
                    var loaded = ModConfig.load(path);
                    require(loaded.hideNumbers && loaded.anchorX == 1 && !loaded.backgroundEnabled, "config round trip");
                    Files.writeString(path, "{not valid json");
                    require(ModConfig.load(path).backgroundEnabled, "broken config falls back");
                    try (var files = Files.list(path.getParent())) {
                        require(files.anyMatch(p -> p.toString().contains(".broken-")), "bad config backup");
                    }
                    ScoreboardTweaks.reset();
                    ChatScreen chat = new ChatScreen("draft test");
                    chat.init(client, width, height);
                    require(((ChatScreenAccessor) chat).scoreboardtweaks$getChatField().getText().equals("draft test"), "chat accessor mixin");
                    client.setScreen(new SettingsScreen(chat));
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000); // Let the real menu render several frames.
                            client.execute(() -> {
                                require(client.currentScreen instanceof SettingsScreen, "settings still open");
                                client.currentScreen.close();
                                require(client.currentScreen instanceof ChatScreen, "returns to chat");
                                require(((ChatScreenAccessor) client.currentScreen).scoreboardtweaks$getChatField().getText().equals("draft test"), "draft preserved");
                                System.out.println("SCOREBOARD_SMOKE_PASS: runtime mixins, sidebar rendering, number width, positions, config, menu and chat draft");
                                client.scheduleStop();
                            });
                        } catch (Exception e) { throw new RuntimeException(e); }
                    }, "scoreboard-smoke-finish").start();
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        }
        private static void require(boolean condition, String message) {
            if (!condition) throw new AssertionError(message);
        }
    }
}
