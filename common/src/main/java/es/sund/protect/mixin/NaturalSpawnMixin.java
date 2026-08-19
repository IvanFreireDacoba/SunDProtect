package es.sund.protect.mixin;

import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.Flags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Engancha ServerLevel#addFreshEntity, el metodo que usa el spawn natural
 * (NaturalSpawner) -- necesario para que los flags de spawn cubran de
 * verdad la aparicion natural de mobs/animales, no solo la generada por
 * comandos o spawners.
 *
 * Las entidades de CustomNPCs (namespace de registro "customnpcs"), los
 * jugadores, los items sueltos (ItemEntity) y los orbes de experiencia
 * (ExperienceOrb) nunca se ven afectados por ningun flag de spawn, ni
 * siquiera por deny-all-spawn -- addFreshEntity tambien es el punto de
 * entrada por el que aparecen items/XP soltados (al tirar un item, al
 * morir un mob...), no solo el spawn natural de mobs/animales que es lo
 * unico que deny-all-spawn/deny-mob-spawn/deny-animal-spawn pretenden
 * cubrir. Items e items dropeados por jugadores solo los controla
 * deny-item-drop (ItemDropMixin); un region con deny-all-spawn activo no
 * debe impedir que un item o su XP lleguen a existir en el mundo.
 */
@Mixin(ServerLevel.class)
public class NaturalSpawnMixin {

    private static final String CUSTOMNPCS_NAMESPACE = "customnpcs";

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void sundProtect$onAddFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player || entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
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
