package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.BaseText;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;

public class BeaconGridCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = literal("beaconGrid").
                requires((player) -> CarpetSettings.beaconChunkLoading).
                executes((c) -> getChunks(c.getSource(), c.getSource().getPlayer()));

        dispatcher.register(literalArgumentBuilder);
    }

    private static List<DimensionType> _allDimensions = new ArrayList<DimensionType>() {{
        add(DimensionType.OVERWORLD);
        add(DimensionType.THE_END);
        add(DimensionType.THE_NETHER);
    }};

    private static int getChunks(ServerCommandSource source, ServerPlayerEntity player) {
        List<BaseText> lst = new ArrayList<>();
        int total = 0;

        for (DimensionType d : _allDimensions) {
            LongSet loaded = source.getMinecraftServer().getWorld(d).getForcedChunks();
            if (loaded == LongSets.EMPTY_SET) {
                continue;
            }

            int chunkTotal = 0;
            List<BaseText> lstChunk = new ArrayList<>();

            for (Long l : loaded) {
                chunkTotal++;
                ChunkPos p = new ChunkPos(l);
                lstChunk.add(Messenger.c(String.format("g %02d: %s", chunkTotal, p.toString())));
            }

            if (0 == chunkTotal) {
                continue;
            }

            lst.add(Messenger.c("w _____________________"));

            lst.add(Messenger.c(String.format("l %d chunk(s) are loaded in %s:", chunkTotal, d.toString())));
            lst.addAll(lstChunk);
            total += chunkTotal;
        }

        if (0 == total) {
            lst.add(Messenger.c("w _____________________"));
            lst.add(Messenger.c("g No loaded chunks"));
        }

        lst.add(Messenger.c("w _____________________"));

        if (0 != total) {
            lst.add(Messenger.c(String.format("r %d total chunk(s) loaded", total)));
        }

        Messenger.send(player, lst);
        return 1;
    }
}
