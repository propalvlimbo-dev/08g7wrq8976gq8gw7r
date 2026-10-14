package dev.scoreboardtweaks.gui;

import dev.scoreboardtweaks.ScoreboardTweaks;
import dev.scoreboardtweaks.config.ModConfig;
import dev.scoreboardtweaks.core.Layout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
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

    /** Draw on the screen layer while editing, so chat and HUD transforms cannot hide/offset the target. */
    public static boolean renderActive(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        visible = false;
        if (client.world == null || client.options.hudHidden) return false;
        var scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = null;
        if (client.player != null && client.player.getScoreboardTeam() != null) {
            var slot = ScoreboardDisplaySlot.fromFormatting(client.player.getScoreboardTeam().getColor());
            if (slot != null) objective = scoreboard.getObjectiveForSlot(slot);
        }
        if (objective == null) objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return false;
        render(context, objective);
        return true;
    }

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
        int baseWidth = contentWidth + 6;
        int baseHeight = (rows.size() + 1) * 9 + 3;
        double scale = Layout.fitScale(config.scale, baseWidth, baseHeight, screenWidth, screenHeight);
        int width = Math.min(screenWidth, (int)Math.ceil(baseWidth * scale));
        int height = Math.min(screenHeight, (int)Math.ceil(baseHeight * scale));
        int x = config.customPosition ? Layout.fromAnchor(config.anchorX, width, screenWidth)
            : Layout.clamp(screenWidth - width - 1, width, screenWidth);
        int y = config.customPosition ? Layout.fromAnchor(config.anchorY, height, screenHeight)
            : Layout.clamp(screenHeight / 2 - (int)Math.round((rows.size() * 6 + 10) * scale), height, screenHeight);
        bounds = new Layout.Bounds(x, y, width, height);
        visible = true;

        // Render and hit-test use the SAME scaled rectangle in GUI coordinates.
        context.enableScissor(x, y, x + width, y + height);
        context.getMatrices().push();
        try {
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale((float)scale, (float)scale, 1);
            if (config.backgroundEnabled && config.backgroundOpacity > 0) {
                context.fill(0, 0, baseWidth, baseHeight, config.backgroundArgb());
            }
            context.drawText(font, title, (baseWidth - font.getWidth(title)) / 2, 2, 0xFFFFFFFF, false);
            int rowY = 12;
            for (Row row : rows) {
                context.drawText(font, row.name(), 3, rowY, 0xFFFFFFFF, false);
                if (!config.hideNumbers) {
                    context.drawText(font, row.score(), baseWidth - 3 - font.getWidth(row.score()), rowY, 0xFFFFFFFF, false);
                }
                rowY += 9;
            }
        } finally {
            context.getMatrices().pop();
            context.disableScissor();
        }
    }

    private record Row(Text name, Text score) {}
}
