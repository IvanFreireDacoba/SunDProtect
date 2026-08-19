package es.sund.protect.gui;

import es.sund.protect.data.ProtectRegion;
import es.sund.protect.data.RegionManager;
import es.sund.protect.flag.FlagGridLayout;
import es.sund.protect.flag.FlagInfo;
import es.sund.protect.flag.Flags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu tipo "cofre grande" (54 huecos, vanilla, sin necesitar mod ni
 * resourcepack en el cliente) que muestra UN item por flag -- cristal
 * verde si esta a true (regla activa, bloquea), cristal rojo si esta a
 * false (regla inactiva, permite). Un solo slot por flag a proposito
 * (pedido explicito del usuario el 2026-08-19: dos slots por flag "es
 * poco coherente"), el nombre/lore del item es lo que identifica de que
 * flag se trata.
 *
 * Layout en rejilla por categoria (FlagGridLayout): cada columna es un
 * dominio (spawn, mecanismos, griefing...), pedido explicito del usuario
 * para poder ver de un vistazo que flags pueden chocar entre si dentro
 * del mismo dominio. Cofre de 6 filas (en vez de 4) porque la columna de
 * spawn puede llegar a 6 flags con Cobblemon cargado.
 *
 * Click izquierdo en el slot de una flag la invierte y refresca TODOS los
 * slots de flags, no solo el pulsado -- porque activar deny-all-spawn
 * puede cambiar otras dos flags en cascada (ver ProtectRegion.setFlag).
 * Click derecho en CUSTOM_SPAWN abre el menu de seleccion de entidades
 * (EntitySpawnPickerMenu) en vez de invertir el flag. Nunca se mueve ni
 * se saca ningun item de verdad, es solo una interfaz.
 *
 * Version 1.21.1: Mojang sustituyo NBT "display"/setHoverName por el
 * sistema de Data Components (ItemStack#set(DataComponentType, T)) --
 * mismo comportamiento final que la 1.20.1, wiring distinto.
 */
public class SundProtectMenu extends ChestMenu {

    private static final int ROWS = FlagGridLayout.ROWS;
    private static final int SIZE = FlagGridLayout.SIZE;

    private final ProtectRegion region;
    private final Container display;
    private final FlagGridLayout layout = new FlagGridLayout();

    public SundProtectMenu(int syncId, Inventory playerInventory, ProtectRegion region) {
        super(MenuType.GENERIC_9x6, syncId, playerInventory, buildContainer(region), ROWS);
        this.region = region;
        this.display = this.getContainer();
    }

    private static Container buildContainer(ProtectRegion region) {
        SimpleContainer container = new SimpleContainer(SIZE);
        FlagGridLayout layout = new FlagGridLayout();
        for (int i = 0; i < SIZE; i++) {
            FlagInfo flag = layout.at(i);
            container.setItem(i, flag != null ? itemFor(region, flag) : filler());
        }
        return container;
    }

    private static ItemStack filler() {
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        return filler;
    }

    private static final int WRAP_WIDTH = 28;

    /** Corta el texto en lineas cortas (rompe por espacios) para que el
     * tooltip no se estire mucho a lo ancho. */
    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > WRAP_WIDTH) {
                lines.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static ItemStack itemFor(ProtectRegion region, FlagInfo flag) {
        boolean denied = region.isFlagDenied(flag.id());
        Item base = denied ? Items.GREEN_STAINED_GLASS : Items.RED_STAINED_GLASS;
        ItemStack stack = new ItemStack(base);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal((denied ? "§c" : "§a") + flag.displayName())
                .withStyle(s -> s.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§6" + flag.category().label()));
        lore.add(Component.literal(""));
        for (String line : wrap(flag.description())) {
            lore.add(Component.literal("§7" + line));
        }
        lore.add(Component.literal(""));
        lore.add(Component.literal("§etrue"));
        lore.add(Component.literal("§7  regla activa,"));
        lore.add(Component.literal("§7  impide la accion"));
        lore.add(Component.literal("§afalse"));
        lore.add(Component.literal("§7  regla inactiva,"));
        lore.add(Component.literal("§7  se permite"));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§fEstado actual: " + (denied ? "§ctrue" : "§afalse")));
        lore.add(Component.literal("§8Click izquierdo: invertir"));
        if (Flags.CUSTOM_SPAWN.equals(flag.id())) {
            lore.add(Component.literal("§8Click derecho: elegir entidades"));
        }

        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    /** Repinta todos los slots de flags -- necesario porque un solo click
     * puede cambiar varios flags a la vez (cascada de deny-all-spawn). */
    private void refreshAllFlagSlots() {
        for (int i = 0; i < SIZE; i++) {
            FlagInfo flag = layout.at(i);
            if (flag != null) {
                this.display.setItem(i, itemFor(region, flag));
            }
        }
        this.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        FlagInfo flag = layout.at(slotId);
        if (flag == null) {
            return; // huecos de relleno o clicks fuera del menu (inventario del jugador) -- ignorar
        }
        if (Flags.CUSTOM_SPAWN.equals(flag.id()) && button == 1 && player instanceof ServerPlayer serverPlayer) {
            openEntityPicker(serverPlayer);
            return;
        }
        boolean currentlyDenied = region.isFlagDenied(flag.id());
        region.setFlag(flag.id(), !currentlyDenied); // invierte true<->false (con cascada si aplica)
        RegionManager.save();
        refreshAllFlagSlots();
    }

    private void openEntityPicker(ServerPlayer player) {
        ProtectRegion region = this.region;
        MenuProvider provider = new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.literal("Entidades: " + region.name);
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new EntitySpawnPickerMenu(syncId, inv, region);
            }
        };
        player.openMenu(provider);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // nunca se puede sacar nada del menu con shift-click
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
