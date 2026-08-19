package es.sund.protect.mixin;

import com.mojang.brigadier.ParseResults;
import es.sund.protect.command.CommandProtectionHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * En 1.20.1, Commands#performCommand devuelve int (el resultado del
 * comando) -- verificado con javap contra el jar real mapeado con
 * mappings oficiales de Mojang de este proyecto, no adivinado. En 1.21.1
 * el mismo metodo devuelve void (reescritura del motor de ejecucion de
 * comandos de Mojang), por eso este mixin no puede vivir en common/ y
 * cada version tiene su propia copia -- la comprobacion real vive en
 * CommandProtectionHelper (es.sund.protect.command, common/), compartida
 * por las dos. Ese helper vivio antes en este mismo paquete
 * (es.sund.protect.mixin) y provoco un crash real en produccion --
 * SpongePowered Mixin trata el paquete declarado en
 * sundprotect.mixins.json de forma especial, y una clase normal ahi
 * dentro puede acabar mal cargada por el propio Mixin. Nunca metas una
 * clase que no sea un @Mixin en este paquete.
 */
@Mixin(Commands.class)
public class CommandProtectionMixin {

    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void sundProtect$onPerformCommand(ParseResults<CommandSourceStack> parseResults, String command,
                                               CallbackInfoReturnable<Integer> cir) {
        if (CommandProtectionHelper.isBlocked(parseResults.getContext().getSource(), command)) {
            cir.setReturnValue(0);
        }
    }
}
