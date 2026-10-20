package jaxon.ric.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;

/** Client-side scope check for the RIC Drops menu's bundle presentation. */
public final class DropsMenuClientPresentation {
    private static final String DROPS_MENU_TITLE = "Configured Drops";
    private static volatile AbstractContainerScreen<?> activeDropsScreen;
    private static volatile BundleContents activeDropsBundleTooltip;
    private static volatile boolean activeDropsTooltip;

    private DropsMenuClientPresentation() {}

    public static boolean isDropsMenuOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui.screen();
        return screen instanceof AbstractContainerScreen<?> containerScreen
                && isDropsMenuScreen(containerScreen);
    }

    public static boolean isDropsMenuScreen(AbstractContainerScreen<?> screen) {
        return isDropsMenuTitle(screen.getTitle().getString());
    }

    /** Records the exact bundle tooltip currently being extracted by a container screen. */
    public static void updateActiveTooltip(AbstractContainerScreen<?> screen, ItemStack stack) {
        boolean inDropsMenu = isDropsMenuScreen(screen);
        activeDropsTooltip = inDropsMenu && stack.is(Items.BUNDLE);
        activeDropsScreen = activeDropsTooltip ? screen : null;
        activeDropsBundleTooltip = activeDropsTooltip ? stack.get(DataComponents.BUNDLE_CONTENTS) : null;
    }

    public static boolean isActiveDropsTooltip() {
        return activeDropsTooltip && isCurrentDropsTooltipScreen();
    }

    public static boolean isActiveDropsBundleTooltip(BundleContents contents) {
        return isActiveDropsTooltip() && activeDropsBundleTooltip == contents;
    }

    private static boolean isDropsMenuTitle(String title) {
        return DROPS_MENU_TITLE.equals(title);
    }

    private static boolean isCurrentDropsTooltipScreen() {
        AbstractContainerScreen<?> screen = activeDropsScreen;
        return screen != null
                && Minecraft.getInstance().gui.screen() == screen
                && isDropsMenuScreen(screen);
    }
}
