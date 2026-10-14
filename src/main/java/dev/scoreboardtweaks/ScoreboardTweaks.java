package dev.scoreboardtweaks;

import dev.scoreboardtweaks.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.nio.file.Path;

public final class ScoreboardTweaks implements ClientModInitializer {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("scoreboard-tweaks.json");
    private static ModConfig config = new ModConfig();

    @Override
    public void onInitializeClient() {
        config = ModConfig.load(CONFIG_PATH);
    }

    public static ModConfig config() { return config; }
    public static void reset() { config = new ModConfig(); }
    public static void save() {
        if (!config.save(CONFIG_PATH)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("scoreboardtweaks.save_error"), false);
            }
        }
    }
}
