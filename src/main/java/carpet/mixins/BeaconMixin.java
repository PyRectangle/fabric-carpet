package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashMap;

@Mixin(Block.class)
public class BeaconMixin {
    private HashMap<ChunkPos, HashMap<BlockPos, Boolean>> chunks = new HashMap<>();

    @Inject(at = @At("TAIL"), method = "onBreak")
    private void afterBreak(World world_1, BlockPos blockPos_1, BlockState blockState_1, PlayerEntity playerEntity_1, CallbackInfo ci) {
        if (!CarpetSettings.beaconChunkLoading || !(blockState_1.getBlock() instanceof BeaconBlock)) {
            return;
        }

        this.setLoad(world_1, blockPos_1, false);
    }

    @Inject(at = @At("TAIL"), method = "onBlockAdded")
    public void afterPlaced(BlockState state, World world, BlockPos pos, BlockState oldState, boolean moved, CallbackInfo ci) {
        System.out.println("placed");
        if (!CarpetSettings.beaconChunkLoading || !(state.getBlock() instanceof BeaconBlock)) {
            return;
        }

        boolean isPowered = world.isReceivingRedstonePower(pos);

        System.out.println(isPowered);

        this.setLoad(world, pos, isPowered);
    }

    @Inject(method = "neighborUpdate", at = @At("TAIL"))
    private void afterNeighborUpdate(BlockState blockState_1, World world_1, BlockPos blockPos_1, Block block_1, BlockPos blockPos_2, boolean boolean_1, CallbackInfo ci) {
        if (!CarpetSettings.beaconChunkLoading || !(blockState_1.getBlock() instanceof BeaconBlock)) {
            return;
        }

        boolean isPowered = world_1.isReceivingRedstonePower(blockPos_1);

        this.setLoad(world_1, blockPos_1, isPowered);
    }

    private void setLoad(World world_1, BlockPos blockPos_1, boolean set) {
        if (null == world_1.getServer()) {
            return;
        }

        boolean canChange = false;
        ChunkPos ch = world_1.getChunk(blockPos_1).getPos();
        HashMap<BlockPos, Boolean> beacons = chunks.get(ch);
        if(set) {
            if(beacons == null) {
                canChange = true;
                beacons = new HashMap<>();
            }
            beacons.put(blockPos_1, true);
            chunks.put(ch, beacons);
        } else {
            if(beacons == null) {
                canChange = true;
            } else {
                beacons.remove(blockPos_1);
                if(beacons.size() == 0) {
                    chunks.remove(ch);
                    canChange = true;
                }
            }
        }
        if(canChange) {
            world_1.getServer().getWorld(world_1.getDimension().getType()).setChunkForced(ch.x, ch.z, set);
        }
    }
}
