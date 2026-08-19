package es.sund.protect;

import es.sund.protect.command.SundProtectCommand;
import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.Flags;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class SundProtect implements ModInitializer {

    public static final String MOD_ID = "sundprotect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * true si el bloque es una trapdoor, vanilla o de otro mod. No basta con
     * "instanceof TrapDoorBlock" -- algunos mods definen su propia trapdoor
     * sin heredar de la clase vanilla (bloque completamente propio, a veces
     * con logica de apertura distinta), asi que se usa tambien el id de
     * registro ("namespace:path") y la clave de traduccion del bloque como
     * respaldo, buscando "trapdoor"/"trampilla" en cualquiera de los dos.
     * Mismo tipo de problema que el bypass ya conocido de CustomNPCs en
     * NaturalSpawnMixin (filtrar por convencion de nombre/namespace cuando
     * el tipo Java no es fiable para cubrir todos los mods).
     */
    private static boolean isTrapdoor(Block block) {
        if (block instanceof TrapDoorBlock) {
            return true;
        }
        String registryPath = BuiltInRegistries.BLOCK.getKey(block).getPath().toLowerCase(Locale.ROOT);
        String translationKey = block.getDescriptionId().toLowerCase(Locale.ROOT);
        return registryPath.contains("trapdoor") || registryPath.contains("trampilla")
                || translationKey.contains("trapdoor") || translationKey.contains("trampilla");
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(RegionManager::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RegionManager.save());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                SundProtectCommand.register(dispatcher));

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player.hasPermissions(2)) {
                return true; // OP siempre puede saltarse la proteccion
            }
            boolean denied = RegionManager.isDenied(world.dimension(), pos, Flags.BREAK);
            return !denied;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof ServerPlayer target)) {
                return InteractionResult.PASS;
            }
            if (player.hasPermissions(2)) {
                return InteractionResult.PASS;
            }
            boolean denied = RegionManager.isDenied(world.dimension(), target.blockPosition(), Flags.PVP);
            return denied ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.hasPermissions(2)) {
                return InteractionResult.PASS;
            }
            var pos = hitResult.getBlockPos();
            Block block = world.getBlockState(pos).getBlock();
            // Trapdoors tienen su propio flag (TRAPDOOR), separado del resto de
            // mecanismos (USE) -- pedido explicito del usuario: bloquear solo el
            // click directo del jugador sobre la trapdoor, sin tocar puertas/
            // botones/etc. Este callback solo dispara con la interaccion directa
            // del jugador, nunca con una activacion por redstone, asi que una
            // trapdoor conectada a un boton/palanca por cable sigue funcionando
            // aunque este flag este activo.
            if (isTrapdoor(block)) {
                if (RegionManager.isDenied(world.dimension(), pos, Flags.TRAPDOOR)) {
                    return InteractionResult.FAIL;
                }
            } else {
                boolean isMechanism = block instanceof DoorBlock
                        || block instanceof FenceGateBlock || block instanceof LeverBlock
                        || block instanceof ButtonBlock || block instanceof PressurePlateBlock;
                if (isMechanism && RegionManager.isDenied(world.dimension(), pos, Flags.USE)) {
                    return InteractionResult.FAIL;
                }
            }
            boolean isContainer = world.getBlockEntity(pos) instanceof MenuProvider;
            if (isContainer && RegionManager.isDenied(world.dimension(), pos, Flags.CONTAINER)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.hasPermissions(2)) {
                return InteractionResult.PASS;
            }
            if (!player.getItemInHand(hand).is(Items.LEAD)) {
                return InteractionResult.PASS;
            }
            boolean denied = RegionManager.isDenied(world.dimension(), entity.blockPosition(), Flags.LEASH);
            return denied ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        LOGGER.info("[SunDProtect] cargado -- {} region(es) al iniciar (se cargan en SERVER_STARTED)",
                RegionManager.all().size());
    }
}
