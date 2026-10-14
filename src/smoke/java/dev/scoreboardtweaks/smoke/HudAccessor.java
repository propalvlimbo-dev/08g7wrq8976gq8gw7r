package dev.scoreboardtweaks.smoke;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(InGameHud.class)
public interface HudAccessor {
    @Invoker("renderScoreboardSidebar")
    void smoke$renderSidebar(DrawContext context, ScoreboardObjective objective);
}
