package dev.scoreboardtweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.scoreboardtweaks.core.Layout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ScoreboardTweaks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean hideNumbers = true;
    public boolean backgroundEnabled = true;
    public int backgroundRgb = 0x000000;
    public int backgroundOpacity = 96;
    public boolean customPosition = false;
    public double anchorX = 1.0;
    public double anchorY = 0.5;

    public void sanitize() {
        backgroundRgb &= 0xFFFFFF;
        backgroundOpacity = Math.max(0, Math.min(255, backgroundOpacity));
        anchorX = Layout.unit(anchorX);
        anchorY = Layout.unit(anchorY);
    }

    public int backgroundArgb() {
        return backgroundEnabled ? (backgroundOpacity << 24) | backgroundRgb : 0;
    }

    public void resetPosition() {
        customPosition = false;
        anchorX = 1.0;
        anchorY = 0.5;
    }

    public static ModConfig load(Path path) {
        if (!Files.exists(path)) return new ModConfig();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ModConfig config = GSON.fromJson(reader, ModConfig.class);
            if (config == null) throw new IOException("Configuration is null");
            config.sanitize();
            return config;
        } catch (Exception e) {
            LOGGER.warn("Cannot read scoreboard config; using defaults", e);
            try {
                Files.copy(path, path.resolveSibling(path.getFileName() + ".broken-" + System.currentTimeMillis()));
            } catch (IOException backupError) {
                LOGGER.warn("Could not back up invalid config", backupError);
            }
            return new ModConfig();
        }
    }

    /** Atomic replacement avoids a half-written config after an interrupted save. */
    public boolean save(Path path) {
        sanitize();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temporary, GSON.toJson(this), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not save scoreboard config", e);
            return false;
        }
    }
}
