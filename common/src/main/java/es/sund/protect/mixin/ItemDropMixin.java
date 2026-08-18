package es.sund.protect.mixin;

import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.Flags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ServerPlayer#drop(boolean) es el metodo real que dispara el
 * ServerboundPlayerActionPacket de "tirar item" (tecla Q) -- confirmado
 * con javap sobre el bytecode de ServerGamePacketListenerImpl#
 * handlePlayerAction en vez de asumirlo (Player#drop solo tiene
 * sobrecargas que reciben un ItemStack ya extraido, no cubren el punto de
 * entrada real). Firma identica en 1.20.1 y 1.21.1.
 */
@Mixin(ServerPlayer.class)
public class ItemDropMixin {

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void sundProtect$onDrop(boolean dropStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.hasPermissions(2)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (RegionManager.isDenied(level.dimension(), player.blockPosition(), Flags.ITEM_DROP)) {
            cir.setReturnValue(false);
        }
    }
}
