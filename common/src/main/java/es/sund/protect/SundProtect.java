package es.sund.protect;

import es.sund.protect.command.SundProtectCommand;
import es.sund.protect.data.RegionManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SundProtect implements ModInitializer {

    public static final String MOD_ID = "sundprotect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(RegionManager::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RegionManager.save());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                SundProtectCommand.register(dispatcher));

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player.hasPermissions(2)) {
                return true; // OP siempre puede, igual que YAWP con op_bypass_flags
            }
            boolean denied = RegionManager.isDenied(world.dimension(), pos, es.sund.protect.flag.Flags.BREAK);
            return !denied;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof ServerPlayer target)) {
                return InteractionResult.PASS;
            }
            if (player.hasPermissions(2)) {
                return InteractionResult.PASS;
            }
            boolean denied = RegionManager.isDenied(world.dimension(), target.blockPosition(), es.sund.protect.flag.Flags.PVP);
            return denied ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        LOGGER.info("[SunDProtect] cargado -- {} region(es) al iniciar (se cargan en SERVER_STARTED)",
                RegionManager.all().size());
    }
}
