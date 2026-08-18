package es.sund.protect.mixin;

import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.Flags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Engancha ServerLevel#addFreshEntity, que es el metodo que usa el
 * spawn natural (NaturalSpawner) -- no addEntity, que es el que engancha
 * YAWP y no cubre el spawn natural (por eso el flag spawning-monster de
 * YAWP no bloqueaba nada de noche, ver RESUME.md).
 *
 * Las entidades de CustomNPCs (namespace de registro "customnpcs") y los
 * jugadores nunca se ven afectados por ningun flag de spawn, ni siquiera
 * por deny-all-spawn.
 */
@Mixin(ServerLevel.class)
public class NaturalSpawnMixin {

    private static final String CUSTOMNPCS_NAMESPACE = "customnpcs";

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void sundProtect$onAddFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player) {
            return;
        }
        EntityType<?> type = entity.getType();
        if (CUSTOMNPCS_NAMESPACE.equals(EntityType.getKey(type).getNamespace())) {
            return;
        }

        ServerLevel level = (ServerLevel) (Object) this;
        var pos = entity.blockPosition();
        var dim = level.dimension();

        if (RegionManager.isDenied(dim, pos, Flags.ALL_SPAWN)) {
            cir.setReturnValue(false);
            return;
        }
        if (entity instanceof Monster && RegionManager.isDenied(dim, pos, Flags.MOB_SPAWN)) {
            cir.setReturnValue(false);
            return;
        }
        if (entity instanceof Animal && RegionManager.isDenied(dim, pos, Flags.ANIMAL_SPAWN)) {
            cir.setReturnValue(false);
        }
    }
}
