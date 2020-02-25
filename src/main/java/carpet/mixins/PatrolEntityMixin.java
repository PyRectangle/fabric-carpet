package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.entity.mob.PatrolEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PatrolEntity.class)
public class PatrolEntityMixin {
    @Shadow
    private boolean patrolling;

    @Inject(method = "isRaidCenterSet", at = @At(value = "HEAD"), cancellable = true)
    private void isRaiding(CallbackInfoReturnable<Boolean> cir) {
        if (CarpetSettings.cancelPillagerPatrols) {
            cir.setReturnValue(false);
        } else {
            cir.setReturnValue(this.patrolling);
        }
    }
}
