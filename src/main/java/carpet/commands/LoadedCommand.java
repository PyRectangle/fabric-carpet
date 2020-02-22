package carpet.commands;

import carpet.mixins.IThreadedAnvilChunkStorage;
import carpet.CarpetSettings;
import carpet.utils.Messenger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.arguments.ColumnPosArgumentType;
import net.minecraft.command.arguments.DimensionArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.BaseText;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class LoadedCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = literal("loaded").
                requires((player) -> CarpetSettings.commandLoaded).
                then(argument("dimension", DimensionArgumentType.dimension()).
                        executes((c) -> getChunks(c.getSource(), c.getSource().getPlayer(), DimensionArgumentType.getDimensionArgument(c, "dimension"))).
                        then(argument("position", ColumnPosArgumentType.columnPos()).
                                executes((c) -> getChunkInfo(c.getSource(), c.getSource().getPlayer(), DimensionArgumentType.getDimensionArgument(c, "dimension"),
                                        ColumnPosArgumentType.getColumnPos(c, "position")))));

        dispatcher.register(literalArgumentBuilder);
    }

    private static int getChunkInfo(ServerCommandSource source, ServerPlayerEntity player, DimensionType dim, ColumnPos pos) {
        List<BaseText> lst = new ArrayList<>();
        ChunkPos chunkPos = new ChunkPos(pos.x >> 4, pos.z >> 4);
        Chunk chunk = source.getMinecraftServer().getWorld(dim).getChunkManager().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
        if (null == chunk) {
            lst.add(Messenger.c("g Chunk is unloaded"));
        } else {
            ThreadedAnvilChunkStorage storage = source.getMinecraftServer().getWorld(dim).method_14178().threadedAnvilChunkStorage;
            ChunkHolder holder = ((IThreadedAnvilChunkStorage) storage).getCurrentChunks().get(chunkPos.toLong());
            lst.add(Messenger.c(String.format("g Chunk %s: ticket lvl: %d (%s)", holder.getPos().toString(), holder.getLevel(), ChunkHolder.getLevelType(holder.getLevel()))));
        }

        Messenger.send(player, lst);
        return 1;
    }

    private static int getChunks(ServerCommandSource source, ServerPlayerEntity player, DimensionType dim) {
        ThreadedAnvilChunkStorage storage = source.getMinecraftServer().getWorld(dim).method_14178().threadedAnvilChunkStorage;

        List<BaseText> lst = new ArrayList<>();

        for (ChunkHolder holder : ((IThreadedAnvilChunkStorage) storage).getCurrentChunks().values()) {
            ChunkHolder.LevelType ll = ChunkHolder.getLevelType(holder.getLevel());
            if (ll == ChunkHolder.LevelType.INACCESSIBLE) {
                continue;
            }

            lst.add(Messenger.c(String.format("w Chunk %s: ticket lvl: %d (%s)", holder.getPos().toString(), holder.getLevel(), ll)));
        }

        lst.add(Messenger.c(String.format("g Total loaded chunks for %s: %d", dim.toString(), lst.size())));

        Messenger.send(player, lst);
        return 1;
    }
}
