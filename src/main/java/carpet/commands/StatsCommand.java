package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
                then(literal("show").then(argument("id selector", StringArgumentType.string())
                        .executes((c) -> showStat(c.getSource(), c.getArgument("id selector", String.class)))));

        dispatcher.register(literalArgumentBuilder);
    }

    private static int printHelp(ServerPlayerEntity player) {
        List<BaseText> msgs = new ArrayList<>();
        msgs.add(Messenger.c("g Use /stats show ID"));
        msgs.add(Messenger.c("c m-<block> §9-- Mined <blocks>"));
        msgs.add(Messenger.c("c u-<item> §9-- Used <items>"));
        msgs.add(Messenger.c("c c-<item> §9-- Crafted <items>"));
        msgs.add(Messenger.c("c b-<item> §9-- Broken <items>"));
        msgs.add(Messenger.c("c p-<item> §9-- Picked up <items>"));
        msgs.add(Messenger.c("c d-<item> §9-- Dropped <items>"));
        msgs.add(Messenger.c("c k-<mob> §9-- Killed <mobs>"));
        msgs.add(Messenger.c("c kb-<mob> §9-- Killed by <mob>"));
        BaseText m = Messenger.c(Messenger.c("c z-<stat> §9-- Custom <stat> (CLICK ME)"));
        m.getStyle().setUnderline(true).setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                "https://minecraft.gamepedia.com/Statistics#List_of_custom_statistic_names"));
        msgs.add(m);
        Messenger.send(player, msgs);
        return 1;
    }

    private static int showStat(ServerCommandSource source, String id) {
        String name = id;
        if (name.length() > 16) {
            String index = "+" + genID(id);
            name = name.substring(0, 16 - index.length()) + index;
        }

        Scoreboard sb = source.getMinecraftServer().getScoreboard();
        ScoreboardObjective so = sb.getObjective(name);
        if (so == null) {
            return 0;
        }

        // 1 is the magic number for sidebar
        sb.setObjectiveSlot(1, so);
        return 1;
    }

    private static String genID(String objectiveName) {
        int sum = 0;
        for (char c : objectiveName.toCharArray()) {
            sum = (sum + ((int) c & 0xF)) ^ ((int) c * 5);
        }

        return String.valueOf(sum);
    }

    private static int hideStats(ServerCommandSource source) {
        source.getMinecraftServer().getScoreboard().setObjectiveSlot(1, null);
        return 1;
    }
}
