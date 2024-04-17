package com.github.may2beez.mayobees.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

public class ScaleUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static int getMouseX(){ return getScaledValue(Mouse.getX()); }
    public static int getMouseY(){ return getScaledValue(mc.displayHeight - Mouse.getY()); }
    public static int getScaledValue(int value){ return value / getGuiScale(); }
    public static int getGuiScale(){ return Math.max(mc.gameSettings.guiScale, 1); }
    public static int getScaledWidth(){ return getScaledValue(mc.displayWidth); }
    public static int getScaledHeight(){ return getScaledValue(mc.displayHeight); }
}
