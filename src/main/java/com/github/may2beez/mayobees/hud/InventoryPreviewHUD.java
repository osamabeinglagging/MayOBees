package com.github.may2beez.mayobees.hud;

import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.BasicHud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import com.github.may2beez.mayobees.util.LogUtils;
import com.github.may2beez.mayobees.util.RenderUtils;
import com.github.may2beez.mayobees.util.ScaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Slot;

public class InventoryPreviewHUD extends BasicHud {
    public final Minecraft mc = Minecraft.getMinecraft();
    @Slider(
            name = "Player Height (%)",
            min = 1f, max = 100f
    )
    public static int inventoryHudPlayerHeightScale = 100;

    @Slider(
            name = "Player Height (%)",
            min = -1000f, max = 1000f
    )
    public static int two = 0;

    public InventoryPreviewHUD() {
        super(true, (float) Minecraft.getMinecraft().displayWidth / 2 - 150, (float) Minecraft.getMinecraft().displayHeight / 2 - 75, 1, true, true, 4, 20, 20, new OneColor(255, 0, 0, 100), false, 0, new OneColor(0, 0, 255));
    }

    @Override
    protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
        drawPlayer(x, y, scale);

        for (int i = 9; i < mc.thePlayer.inventoryContainer.inventorySlots.size(); i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot == null || !slot.getHasStack()) continue;
            int posX = (int) (x + 16 * (i % 9));
            int posY = (int) (y + getHeight(scale, false) / 4 * ((i / 9) - 1));
            String size = slot.getStack().stackSize == 1 ? "" : String.valueOf(slot.getStack().stackSize);
            RenderUtils.drawItemStack(slot.getStack(), posX, posY, size);
        }
    }

    @Override
    protected float getWidth(float scale, boolean example) {
        return ScaleUtils.getScaledValue(400) * scale;
    }

    @Override
    protected float getHeight(float scale, boolean example) {
        return ScaleUtils.getScaledValue(150) * scale;
    }

    private void drawPlayer(float x, float y, float scale) {
        int playerScale = (int) ((getHeight(scale, false) * (inventoryHudPlayerHeightScale / 100f)) / 1.8);
        float playerWidth = playerScale * 0.6f;
        int playerXPos = (int) (x + getWidth(scale, false) - playerWidth);
        int playerYPos = (int) (y + (getHeight(scale, false) + (playerWidth * 3)) / 2f);
        GuiInventory.drawEntityOnScreen(playerXPos, playerYPos, playerScale, playerXPos - ScaleUtils.getMouseX(), playerYPos - playerWidth * 3 - ScaleUtils.getMouseY(), this.mc.thePlayer);
    }
}
