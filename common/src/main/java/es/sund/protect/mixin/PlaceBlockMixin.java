package es.sund.protect.mixin;

import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.Flags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * useItemOn es el punto de entrada del lado servidor para "usar un item
 * sobre un bloque" -- cubre tanto colocar bloques (BlockItem) como
 * cubos de liquido (BucketItem, tanto para verter como para recoger),
 * en un unico hook. Se deniega si la posicion objetivo cae en una
 * region con deny-place, salvo para OPs.
 */
@Mixin(ServerPlayerGameMode.class)
public class PlaceBlockMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void sundProtect$onUseItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
                                          BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.hasPermissions(2)) {
            return;
        }
        boolean isBlockOrBucket = stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem;
        if (!isBlockOrBucket) {
            return;
        }
        BlockPos targetPos = hitResult.getBlockPos().relative(hitResult.getDirection());
        BlockPos sourcePos = hitResult.getBlockPos();
        if (RegionManager.isDenied(level.dimension(), targetPos, Flags.PLACE)
                || RegionManager.isDenied(level.dimension(), sourcePos, Flags.PLACE)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
