package es.sund.protect.mixin;

import es.sund.protect.data.ProtectRegion;
import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.Flags;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;

/**
 * Logica compartida por los dos CommandProtectionMixin especificos de
 * version (1.20.1/1.21.1) -- Commands#performCommand cambia de firma entre
 * versiones (int en 1.20.1, void en 1.21.1, ver comentario en cada mixin),
 * asi que el mixin en si no puede vivir en common/, pero la comprobacion
 * real de flags si es identica en las dos.
 */
final class CommandProtectionHelper {

    private CommandProtectionHelper() {
    }

    /**
     * true si el comando debe bloquearse -- ya envia el mensaje de fallo al
     * jugador si es asi, el mixin llamante solo tiene que cancelar.
     */
    static boolean isBlocked(CommandSourceStack source, String command) {
        if (!(source.getEntity() instanceof ServerPlayer player) || player.hasPermissions(2)) {
            return false;
        }
        Optional<ProtectRegion> maybe = RegionManager.findResponsibleRegion(player.level().dimension(), player.blockPosition());
        if (maybe.isEmpty()) {
            return false;
        }
        ProtectRegion region = maybe.get();
        String base = baseCommand(command);

        boolean blockedByAllowlist = region.isFlagDenied(Flags.ALLOWED_COMMANDS)
                && region.allowedCommandsList.stream().noneMatch(c -> c.equalsIgnoreCase(base));
        boolean blockedByDenylist = region.isFlagDenied(Flags.DENIED_COMMANDS)
                && region.deniedCommandsList.stream().anyMatch(c -> c.equalsIgnoreCase(base));

        if (blockedByAllowlist || blockedByDenylist) {
            source.sendFailure(Component.literal("Una fuerza misteriosa te impide usar ese comando"));
            return true;
        }
        return false;
    }

    private static String baseCommand(String command) {
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        int spaceIdx = trimmed.indexOf(' ');
        String base = spaceIdx == -1 ? trimmed : trimmed.substring(0, spaceIdx);
        return base.toLowerCase(Locale.ROOT);
    }
}
