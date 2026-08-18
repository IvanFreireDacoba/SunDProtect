package es.sund.protect.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import es.sund.protect.data.ProtectRegion;
import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.FlagInfo;
import es.sund.protect.flag.Flags;
import es.sund.protect.gui.SundProtectMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public final class SundProtectCommand {

    private static final SuggestionProvider<CommandSourceStack> REGION_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(RegionManager.all().stream().map(r -> r.name), builder);

    private static final SuggestionProvider<CommandSourceStack> FLAG_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Flags.ALL, builder);

    private SundProtectCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sundprotect")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", word())
                                .then(Commands.argument("x1", IntegerArgumentType.integer())
                                        .then(Commands.argument("y1", IntegerArgumentType.integer())
                                                .then(Commands.argument("z1", IntegerArgumentType.integer())
                                                        .then(Commands.argument("x2", IntegerArgumentType.integer())
                                                                .then(Commands.argument("y2", IntegerArgumentType.integer())
                                                                        .then(Commands.argument("z2", IntegerArgumentType.integer())
                                                                                .executes(SundProtectCommand::create)))))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", word()).suggests(REGION_SUGGESTIONS)
                                .executes(SundProtectCommand::remove)))
                .then(Commands.literal("list")
                        .executes(SundProtectCommand::list))
                .then(Commands.literal("info")
                        .then(Commands.argument("name", word()).suggests(REGION_SUGGESTIONS)
                                .executes(SundProtectCommand::info)))
                .then(Commands.literal("flag")
                        .then(Commands.argument("name", word()).suggests(REGION_SUGGESTIONS)
                                .executes(SundProtectCommand::openFlagMenu)
                                .then(Commands.argument("flag", word()).suggests(FLAG_SUGGESTIONS)
                                        .then(Commands.argument("denied", BoolArgumentType.bool())
                                                .executes(SundProtectCommand::setFlag)))))
        );
    }

    private static int create(CommandContext<CommandSourceStack> ctx) {
        String name = getString(ctx, "name");
        if (RegionManager.exists(name)) {
            ctx.getSource().sendFailure(Component.literal("Ya existe una region llamada '" + name + "'."));
            return 0;
        }
        BlockPos p1 = new BlockPos(
                IntegerArgumentType.getInteger(ctx, "x1"),
                IntegerArgumentType.getInteger(ctx, "y1"),
                IntegerArgumentType.getInteger(ctx, "z1"));
        BlockPos p2 = new BlockPos(
                IntegerArgumentType.getInteger(ctx, "x2"),
                IntegerArgumentType.getInteger(ctx, "y2"),
                IntegerArgumentType.getInteger(ctx, "z2"));
        if (p1.equals(p2)) {
            ctx.getSource().sendFailure(Component.literal("Los dos vertices no pueden ser el mismo punto."));
            return 0;
        }
        ProtectRegion region = new ProtectRegion(name, ctx.getSource().getLevel().dimension(), p1, p2);
        for (FlagInfo flag : Flags.ALL_INFO) {
            if (flag.defaultDenied()) {
                region.setFlag(flag.id(), true); // true = regla activa, bloquea
            }
        }
        RegionManager.add(region);
        long volume = (long) (region.maxX - region.minX + 1) * (region.maxY - region.minY + 1) * (region.maxZ - region.minZ + 1);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Region '" + name + "' creada: (" + region.minX + "," + region.minY + "," + region.minZ + ") -> ("
                        + region.maxX + "," + region.maxY + "," + region.maxZ + ") [" + volume + " bloques]. "
                        + "Todos los flags activos (true) por defecto -- usa /sundprotect flag " + name
                        + " para abrir el menu y ajustar."
        ), true);
        return 1;
    }

    private static int openFlagMenu(CommandContext<CommandSourceStack> ctx) {
        String name = getString(ctx, "name");
        Optional<ProtectRegion> maybe = RegionManager.get(name);
        if (maybe.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No existe una region llamada '" + name + "'."));
            return 0;
        }
        ServerPlayer player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Este menu solo se puede abrir como jugador, no desde consola."));
            return 0;
        }
        ProtectRegion region = maybe.get();
        MenuProvider provider = new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.literal("Flags: " + region.name);
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, net.minecraft.world.entity.player.Player p) {
                return new SundProtectMenu(syncId, inv, region);
            }
        };
        player.openMenu(provider);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        String name = getString(ctx, "name");
        if (RegionManager.remove(name)) {
            ctx.getSource().sendSuccess(() -> Component.literal("Region '" + name + "' eliminada."), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("No existe una region llamada '" + name + "'."));
        return 0;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var regions = RegionManager.all();
        if (regions.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No hay regiones creadas."), false);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(regions.size() + " region(es): "
                + String.join(", ", regions.stream().map(r -> r.name).toList())), false);
        return regions.size();
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        String name = getString(ctx, "name");
        Optional<ProtectRegion> maybe = RegionManager.get(name);
        if (maybe.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No existe una region llamada '" + name + "'."));
            return 0;
        }
        ProtectRegion r = maybe.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Region '").append(r.name).append("' en ").append(r.dimension).append("\n");
        sb.append("  (").append(r.minX).append(",").append(r.minY).append(",").append(r.minZ)
                .append(") -> (").append(r.maxX).append(",").append(r.maxY).append(",").append(r.maxZ).append(")\n");
        sb.append("  Flags (true = activa/bloquea, false = inactiva/permite):\n");
        if (r.flags.isEmpty()) {
            sb.append("  (ninguno puesto, todo permitido)");
        } else {
            for (var entry : r.flags.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
            }
        }
        String msg = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int setFlag(CommandContext<CommandSourceStack> ctx) {
        String name = getString(ctx, "name");
        String flag = getString(ctx, "flag");
        // true = regla activa (bloquea/impide), false = regla inactiva (permite).
        // Sentido directo, sin negaciones -- ProtectRegion.setFlag guarda
        // exactamente este mismo valor.
        boolean denied = BoolArgumentType.getBool(ctx, "denied");
        Optional<ProtectRegion> maybe = RegionManager.get(name);
        if (maybe.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No existe una region llamada '" + name + "'."));
            return 0;
        }
        if (!Flags.ALL.contains(flag)) {
            ctx.getSource().sendFailure(Component.literal("Flag desconocido '" + flag + "'. Validos: "
                    + String.join(", ", Flags.ALL)));
            return 0;
        }
        maybe.get().setFlag(flag, denied);
        RegionManager.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Flag '" + flag + "' en '" + name + "' puesto a " + denied
                        + " (" + (denied ? "regla activa, impide la accion" : "regla inactiva, se permite") + ")."), true);
        return 1;
    }
}
