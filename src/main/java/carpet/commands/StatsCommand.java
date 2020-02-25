package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.text.ClickEvent;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class StatsCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = literal("stats").
                requires((player) -> CarpetSettings.commandStats).
                then(literal("help").executes((c) -> printHelp(c.getSource().getPlayer()))).
                then(literal("hide").executes((c) -> hideStats(c.getSource()))).
                then(literal("show").then(argument("id selector", IntegerArgumentType.integer())
                        .executes((c) -> showStat(c.getSource(), c.getArgument("id selector", Integer.class)))));

        dispatcher.register(literalArgumentBuilder);
    }

    private static int printHelp(ServerPlayerEntity player) {
        List<BaseText> msgs = new ArrayList<>();
        msgs.add(Messenger.c("g Use /stats show ID"));
        BaseText m = Messenger.c("b List of available IDs");
        m.getStyle().setUnderline(true).setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                "https://github.com/JimmyCushnie/Minecraft-Stat-Tracker/blob/d2f40d803b3ce3ef9cb28b72ab4ad8cea01bbb6d/Guide.txt"));
        msgs.add(m);
        Messenger.send(player, msgs);
        return 1;
    }

    private static int showStat(ServerCommandSource source, int id) {
        Scoreboard sb = source.getMinecraftServer().getScoreboard();
        ScoreboardObjective so = sb.getObjective(String.valueOf(id));
        if (so == null) {
            return 0;
        }

        // 1 is the magic number for sidebar
        sb.setObjectiveSlot(1, so);
        return 1;
    }

    private static int hideStats(ServerCommandSource source) {
        source.getMinecraftServer().getScoreboard().setObjectiveSlot(1, null);
        return 1;
    }
}
