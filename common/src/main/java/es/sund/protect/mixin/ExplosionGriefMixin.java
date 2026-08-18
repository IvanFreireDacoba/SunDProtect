package es.sund.protect.mixin;

import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.Flags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

/**
 * Filtra la lista de bloques a destruir de cualquier explosion (creeper,
 * TNT, cargas de viento, etc.) despues de que explode() la calcula, antes
 * de que finalizeExplosion() la aplique -- usa el getter publico
 * getToBlow() de Explosion, sin necesidad de capturar variables locales
 * del bytecode (mas fragil, es lo que hace YAWP). Cubre "griefing" para
 * todo tipo de explosion, no solo creepers.
 */
@Mixin(Explosion.class)
public class ExplosionGriefMixin {

    @Shadow
    private Level level;

    @Inject(method = "explode", at = @At("RETURN"))
    private void sundProtect$filterExplosionBlocks(CallbackInfo ci) {
        Explosion self = (Explosion) (Object) this;
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        Iterator<BlockPos> it = self.getToBlow().iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (RegionManager.isDenied(this.level.dimension(), pos, Flags.MOBGRIEF)) {
                it.remove();
            }
        }
    }
}
