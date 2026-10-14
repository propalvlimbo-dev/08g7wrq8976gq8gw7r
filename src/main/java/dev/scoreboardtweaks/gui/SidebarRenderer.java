package dev.scoreboardtweaks.gui;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.config.ModConfig;
import dev.scoreboardtweaks.core.Layout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;

public final class SidebarRenderer {
    private static final Comparator<ScoreboardEntry> ORDER = Comparator
        .comparingInt(ScoreboardEntry::value).reversed()
        .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);
    private static Layout.Bounds bounds;
    private static boolean visible;

    private SidebarRenderer() {}

    public static void beginFrame() {
        visible = false;
        SidebarInteraction.checkScreen();
    }

    public static Layout.Bounds bounds() { return visible ? bounds : null; }

    public static void render(DrawContext context, ScoreboardObjective objective) {
        MinecraftClient client = MinecraftClient.getInstance();
        ModConfig config = ScoreboardTweaks.config();
        var font = client.textRenderer;
        var scoreboard = objective.getScoreboard();
        var format = objective.getNumberFormatOr(StyledNumberFormat.RED);
        List<Row> rows = scoreboard.getScoreboardEntries(objective).stream()
            .filter(entry -> !entry.hidden()).sorted(ORDER).limit(15)
            .map(entry -> new Row(Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name()),
                config.hideNumbers ? Text.empty() : entry.formatted(format)))
            .toList();

        Text title = objective.getDisplayName();
        int contentWidth = font.getWidth(title);
        for (Row row : rows) {
            int scoreWidth = font.getWidth(row.score());
            contentWidth = Math.max(contentWidth, font.getWidth(row.name()) + (scoreWidth > 0 ? scoreWidth + 4 : 0));
        }
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        // Clip malicious/oversized server text rather than leaving an ungrabbable off-screen board.
        int width = Math.min(contentWidth + 6, screenWidth);
        int height = Math.min((rows.size() + 1) * 9 + 3, screenHeight);
        int x = config.customPosition ? Layout.fromAnchor(config.anchorX, width, screenWidth)
            : Layout.clamp(screenWidth - width - 1, width, screenWidth);
        int y = config.customPosition ? Layout.fromAnchor(config.anchorY, height, screenHeight)
            : Layout.clamp(screenHeight / 2 - rows.size() * 6 - 10, height, screenHeight);
        bounds = new Layout.Bounds(x, y, width, height);
        visible = true;

        context.enableScissor(x, y, x + width, y + height);
        if (config.backgroundEnabled && config.backgroundOpacity > 0) {
            context.fill(x, y, x + width, y + height, config.backgroundArgb());
        }
        context.drawText(font, title, x + (width - font.getWidth(title)) / 2, y + 2, 0xFFFFFFFF, false);
        int rowY = y + 12;
        for (Row row : rows) {
            context.drawText(font, row.name(), x + 3, rowY, 0xFFFFFFFF, false);
            if (!config.hideNumbers) {
                context.drawText(font, row.score(), x + width - 3 - font.getWidth(row.score()), rowY, 0xFFFFFFFF, false);
            }
            rowY += 9;
        }
        context.disableScissor();
    }

    private record Row(Text name, Text score) {}
}
