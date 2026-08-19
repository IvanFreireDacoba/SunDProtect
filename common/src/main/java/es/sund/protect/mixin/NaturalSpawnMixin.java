package es.sund.protect.mixin;

import es.sund.protect.compat.CobblemonSupport;
import es.sund.protect.data.ProtectRegion;
import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.Flags;
import es.sund.protect.flag.SpawnExemptions;
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

import java.util.Optional;

/**
 * Engancha ServerLevel#addFreshEntity, el metodo que usa el spawn natural
 * (NaturalSpawner) -- necesario para que los flags de spawn cubran de
 * verdad la aparicion natural de mobs/animales, no solo la generada por
 * comandos o spawners. Cualquier sistema de spawn propio de un mod
 * (Cobblemon incluido) tiene que pasar por aqui tarde o temprano -- es el
 * unico punto vanilla para dar de alta una entidad de verdad en el mundo.
 *
 * Los jugadores (sin EntityType propio, se comprueban con instanceof) y
 * los tipos exentos de SpawnExemptions (items sueltos, orbes de
 * experiencia, NPCs de CustomNPCs) nunca se ven afectados por ningun flag
 * de spawn, ni siquiera por deny-all-spawn -- ver el javadoc de
 * SpawnExemptions para el motivo completo.
 *
 * Una sola resolucion de "region responsable" por llamada (en vez de una
 * por flag) -- ademas de ahorrar busquedas repetidas, es necesario para
 * poder leer directamente la lista de CUSTOM_SPAWN de esa region.
 */
@Mixin(ServerLevel.class)
public class NaturalSpawnMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void sundProtect$onAddFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player) {
            return;
        }
        EntityType<?> type = entity.getType();
        if (SpawnExemptions.isExempt(type)) {
            return;
        }

        ServerLevel level = (ServerLevel) (Object) this;
        var pos = entity.blockPosition();
        var dim = level.dimension();

        Optional<ProtectRegion> regionOpt = RegionManager.findResponsibleRegion(dim, pos);
        if (regionOpt.isEmpty()) {
            return;
        }
        ProtectRegion region = regionOpt.get();

        if (region.isFlagDenied(Flags.ALL_SPAWN)) {
            cir.setReturnValue(false);
            return;
        }

        boolean isPokemon = CobblemonSupport.isPokemonEntity(entity);
        if (isPokemon) {
            // PokemonEntity hereda de Animal (ver CobblemonSupport) -- se
            // controla EXCLUSIVAMENTE con estos dos flags, nunca con
            // deny-mob-spawn/deny-animal-spawn, para que activar el flag
            // generico de animales no bloquee Pokemon por sorpresa.
            boolean custom = CobblemonSupport.isCustomSpecies(entity);
            String flag = custom ? Flags.SPAWN_CUSTOM_POKEMONS : Flags.SPAWN_POKEMONS;
            if (region.isFlagDenied(flag)) {
                cir.setReturnValue(false);
            }
            return;
        }

        if (entity instanceof Monster && region.isFlagDenied(Flags.MOB_SPAWN)) {
            cir.setReturnValue(false);
            return;
        }
        if (entity instanceof Animal && region.isFlagDenied(Flags.ANIMAL_SPAWN)) {
            cir.setReturnValue(false);
            return;
        }
        if (region.isFlagDenied(Flags.CUSTOM_SPAWN)) {
            String key = EntityType.getKey(type).toString();
            if (region.customSpawnList.stream().anyMatch(id -> id.equalsIgnoreCase(key))) {
                cir.setReturnValue(false);
            }
        }
    }
}
