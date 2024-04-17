package com.github.may2beez.mayobees.module.impl.utils;

import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Test implements IModuleActive {
    Minecraft mc = Minecraft.getMinecraft();
    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @SubscribeEvent
    public void onGuiOpen(GuiScreenEvent.InitGuiEvent event){
    }
}
