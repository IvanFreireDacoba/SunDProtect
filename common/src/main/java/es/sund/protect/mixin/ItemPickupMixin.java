package es.sund.protect.mixin;

import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.Flags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ItemEntity#playerTouch es el punto de entrada vanilla cuando un
 * jugador colisiona con un item en el suelo (tanto recogida automatica al
 * andar por encima como cualquier otro disparo de este metodo). Firma
 * identica en 1.20.1 y 1.21.1.
 */
@Mixin(ItemEntity.class)
public class ItemPickupMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void sundProtect$onPlayerTouch(Player player, CallbackInfo ci) {
        if (player.level().isClientSide() || player.hasPermissions(2)) {
            return;
        }
        ItemEntity self = (ItemEntity) (Object) this;
        if (RegionManager.isDenied(player.level().dimension(), self.blockPosition(), Flags.ITEM_PICKUP)) {
            ci.cancel();
        }
    }
}
