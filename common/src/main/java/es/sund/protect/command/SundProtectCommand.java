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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
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
                .then(Commands.literal("priority")
                        .then(Commands.argument("name", word()).suggests(REGION_SUGGESTIONS)
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 99))
                                        .executes(SundProtectCommand::setPriority))))
                .then(Commands.literal("command")
                        .then(Commands.argument("name", word()).suggests(REGION_SUGGESTIONS)
                                .then(Commands.literal("allow")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("command", word())
                                                        .executes(ctx -> addCommand(ctx, true))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("command", word())
                                                        .executes(ctx -> removeCommand(ctx, true)))))
                                .then(Commands.literal("deny")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("command", word())
                                                        .executes(ctx -> addCommand(ctx, false))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("command", word())
                                                        .executes(ctx -> removeCommand(ctx, false)))))))
                .then(Commands.literal("help")
                        .executes(ctx -> help(ctx, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> help(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
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
        long volume = region.volume();
        List<ProtectRegion> overlapping = RegionManager.findOverlapping(region);
        StringBuilder msg = new StringBuilder();
        msg.append("Region '").append(name).append("' creada: (")
                .append(region.minX).append(",").append(region.minY).append(",").append(region.minZ).append(") -> (")
                .append(region.maxX).append(",").append(region.maxY).append(",").append(region.maxZ)
                .append(") [").append(volume).append(" bloques]. ")
                .append("Prioridad por defecto: ").append(region.priority)
                .append(". Todos los flags activos (true) por defecto -- usa /sundprotect flag ").append(name)
                .append(" para abrir el menu y ajustar.");
        if (!overlapping.isEmpty()) {
            msg.append(" Area superpuesta con: ")
                    .append(String.join(", ", overlapping.stream().map(r -> r.name).toList()));
        }
        String out = msg.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(out), true);
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
        sb.append("  Prioridad: ").append(r.priority).append(" (1 = maxima, 99 = minima/por defecto)\n");
        List<ProtectRegion> overlapping = RegionManager.findOverlapping(r);
        if (!overlapping.isEmpty()) {
            sb.append("  Area superpuesta con: ")
                    .append(String.join(", ", overlapping.stream().map(o -> o.name).toList())).append("\n");
        }
        if (!r.allowedCommandsList.isEmpty()) {
            sb.append("  Lista blanca (allowed-commands): ")
                    .append(String.join(", ", r.allowedCommandsList)).append("\n");
        }
        if (!r.deniedCommandsList.isEmpty()) {
            sb.append("  Lista negra (denied-commands): ")
                    .append(String.join(", ", r.deniedCommandsList)).append("\n");
        }
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

    private static int setPriority(CommandContext<CommandSourceStack> ctx) {
        String name = getString(ctx, "name");
        int value = IntegerArgumentType.getInteger(ctx, "value");
        Optional<ProtectRegion> maybe = RegionManager.get(name);
        if (maybe.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No existe una region llamada '" + name + "'."));
            return 0;
        }
        maybe.get().priority = value;
        RegionManager.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Prioridad de '" + name + "' puesta a " + value
                        + " (1 = maxima, se aplica primero en zonas solapadas; 99 = minima)."), true);
        return 1;
    }

    private static int addCommand(CommandContext<CommandSourceStack> ctx, boolean allowList) {
        String name = getString(ctx, "name");
        String command = getString(ctx, "command").toLowerCase(Locale.ROOT);
        Optional<ProtectRegion> maybe = RegionManager.get(name);
        if (maybe.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No existe una region llamada '" + name + "'."));
            return 0;
        }
        List<String> list = allowList ? maybe.get().allowedCommandsList : maybe.get().deniedCommandsList;
        String listLabel = allowList ? "blanca (allowed-commands)" : "negra (denied-commands)";
        if (list.stream().anyMatch(c -> c.equalsIgnoreCase(command))) {
            ctx.getSource().sendFailure(Component.literal(
                    "'" + command + "' ya esta en la lista " + listLabel + " de '" + name + "'."));
            return 0;
        }
        list.add(command);
        RegionManager.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "'" + command + "' añadido a la lista " + listLabel + " de '" + name + "'."), true);
        return 1;
    }

    private static int removeCommand(CommandContext<CommandSourceStack> ctx, boolean allowList) {
        String name = getString(ctx, "name");
        String command = getString(ctx, "command").toLowerCase(Locale.ROOT);
        Optional<ProtectRegion> maybe = RegionManager.get(name);
        if (maybe.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No existe una region llamada '" + name + "'."));
            return 0;
        }
        List<String> list = allowList ? maybe.get().allowedCommandsList : maybe.get().deniedCommandsList;
        String listLabel = allowList ? "blanca (allowed-commands)" : "negra (denied-commands)";
        if (!list.removeIf(c -> c.equalsIgnoreCase(command))) {
            ctx.getSource().sendFailure(Component.literal(
                    "'" + command + "' no estaba en la lista " + listLabel + " de '" + name + "'."));
            return 0;
        }
        RegionManager.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "'" + command + "' quitado de la lista " + listLabel + " de '" + name + "'."), true);
        return 1;
    }

    private record HelpEntry(String usage, String description) {
    }

    private static final int HELP_PAGE_SIZE = 4;

    private static final List<HelpEntry> HELP_ENTRIES = List.of(
            new HelpEntry("/sundprotect create <nombre> <x1> <y1> <z1> <x2> <y2> <z2>",
                    "Crea una region nueva entre dos vertices. Todos los flags quedan activos (bloqueando) por defecto y la prioridad queda en 99."),
            new HelpEntry("/sundprotect remove <nombre>",
                    "Elimina una region existente."),
            new HelpEntry("/sundprotect list",
                    "Lista los nombres de todas las regiones creadas."),
            new HelpEntry("/sundprotect info <nombre>",
                    "Muestra los limites, la dimension, la prioridad, con que otras regiones se solapa y los flags de una region."),
            new HelpEntry("/sundprotect flag <nombre>",
                    "Abre un menu de inventario para activar/desactivar los flags de la region con un click."),
            new HelpEntry("/sundprotect flag <nombre> <flag> <true|false>",
                    "Activa o desactiva un flag concreto por comando, sin abrir el menu."),
            new HelpEntry("/sundprotect priority <nombre> <1-99>",
                    "Cambia la prioridad de la region (1 = maxima, gana en zonas solapadas; 99 = minima, valor por defecto)."),
            new HelpEntry("/sundprotect command <nombre> allow add|remove <comando>",
                    "Añade o quita un comando de la lista blanca (flag allowed-commands): con el flag activo, solo los comandos de esta lista se pueden usar en la region."),
            new HelpEntry("/sundprotect command <nombre> deny add|remove <comando>",
                    "Añade o quita un comando de la lista negra (flag denied-commands): con el flag activo, los comandos de esta lista quedan bloqueados en la region."),
            new HelpEntry("/sundprotect help [pagina]",
                    "Muestra esta ayuda, con botones para cambiar de pagina.")
    );

    private static int help(CommandContext<CommandSourceStack> ctx, int page) {
        int totalPages = (int) Math.ceil(HELP_ENTRIES.size() / (double) HELP_PAGE_SIZE);
        if (page < 1 || page > totalPages) {
            ctx.getSource().sendFailure(Component.literal("Pagina invalida. Hay " + totalPages + " pagina(s)."));
            return 0;
        }
        MutableComponent msg = Component.literal("=== SunDProtect - Ayuda (pagina " + page + "/" + totalPages + ") ===\n")
                .withStyle(ChatFormatting.GOLD);
        int from = (page - 1) * HELP_PAGE_SIZE;
        int to = Math.min(from + HELP_PAGE_SIZE, HELP_ENTRIES.size());
        for (int i = from; i < to; i++) {
            HelpEntry entry = HELP_ENTRIES.get(i);
            msg.append(Component.literal(entry.usage() + "\n").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("  " + entry.description() + "\n").withStyle(ChatFormatting.GRAY));
        }
        MutableComponent nav = Component.literal("");
        if (page > 1) {
            int prev = page - 1;
            nav.append(Component.literal("[<< Anterior]").withStyle(style -> style.withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sundprotect help " + prev))));
        }
        if (page > 1 && page < totalPages) {
            nav.append(Component.literal("   "));
        }
        if (page < totalPages) {
            int next = page + 1;
            nav.append(Component.literal("[Siguiente >>]").withStyle(style -> style.withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sundprotect help " + next))));
        }
        msg.append(nav);
        Component out = msg;
        ctx.getSource().sendSuccess(() -> out, false);
        return 1;
    }
}
