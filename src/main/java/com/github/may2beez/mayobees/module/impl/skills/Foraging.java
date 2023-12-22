package com.github.may2beez.mayobees.module.impl.skills;

import com.github.may2beez.mayobees.config.MayOBeesConfig;
import com.github.may2beez.mayobees.handler.RotationHandler;
import com.github.may2beez.mayobees.module.IModuleActive;
import com.github.may2beez.mayobees.util.*;
import com.github.may2beez.mayobees.util.helper.RotationConfiguration;
import com.github.may2beez.mayobees.util.helper.Target;
import com.github.may2beez.mayobees.util.helper.Timer;
import com.google.common.base.Splitter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Foraging implements IModuleActive {
    private static Foraging instance;

    public static Foraging getInstance() {
        if (instance == null) {
            instance = new Foraging();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Foraging";
    }

    public static Minecraft mc = Minecraft.getMinecraft();

    private enum MacroState {
        LOOK, PLACE, PLACE_BONE, BREAK, FIND_ROD, FIND_BONE, THROW_ROD, THROW_BREAK_DELAY, SWITCH
    }

    private MacroState macroState = MacroState.LOOK;
    private MacroState lastState = null;

    public Vec3 currentTarget;
    private final ArrayList<Vec3> dirtBlocks = new ArrayList<>();

    private final Timer stuckTimer = new Timer();
    private boolean stuck = false;

    private boolean enabled = false;
    private long lastBreakTime = 0;

    public boolean isRunning() {
        return enabled;
    }

    @Override
    public void onEnable() {
        enabled = true;
        dirtBlocks.clear();
        dirtBlocks.addAll(getDirts());
        currentTarget = null;
        macroState = MacroState.LOOK;
        startedAt = System.currentTimeMillis();
        earnedXp = 0;
        lastBreakTime = 0;
        stuckTimer.schedule();
        stuck = false;
        updateXpTimer.schedule();
        if (MayOBeesConfig.mouseUngrab)
            UngrabUtils.ungrabMouse();
    }

    @Override
    public void onDisable() {
        KeyBindUtils.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindAttack, false);
        KeyBindUtils.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindUseItem, false);
        enabled = false;
        UngrabUtils.regrabMouse();
    }

    private static final Timer updateXpTimer = new Timer();
    private static final Timer waitTimer = new Timer();
    private static final Timer waitAfterFinishTimer = new Timer();
    private static double xpPerHour = 0;

    public String[] drawFunction() {
        String[] textToDraw = new String[4];
        if (updateXpTimer.hasPassed(100)) {
            xpPerHour = earnedXp / ((System.currentTimeMillis() - startedAt) / 3600000.0);
            updateXpTimer.reset();
        }
        textToDraw[0] = "§r§lForaging Macro";
        textToDraw[1] = "§r§lState: §f" + macroState;
        textToDraw[2] = "§r§lXP/H: §f" + String.format("%.2f", xpPerHour);
        textToDraw[3] = "§r§lXP Since start: §f" + String.format("%.2f", earnedXp);
        return textToDraw;
    }

    private long startedAt = 0;
    private double earnedXp = 0;
    private final Splitter SPACE_SPLITTER = Splitter.on("  ").omitEmptyStrings().trimResults();
    private final Pattern SKILL_PATTERN = Pattern.compile("\\+([\\d.]+)\\s+([A-Za-z]+)\\s+\\((\\d+(\\.\\d+)?)%\\)");

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onChatReceive(ClientChatReceivedEvent event) {
        if (!isRunning()) return;
        if (event.type != 2) return;

        String actionBar = StringUtils.stripControlCodes(event.message.getUnformattedText());

        List<String> components = SPACE_SPLITTER.splitToList(actionBar);

        for (String component : components) {
            Matcher matcher = SKILL_PATTERN.matcher(component);
            if (matcher.matches()) {
                String addedXp = matcher.group(1);
                String skillName = matcher.group(2);
                if (skillName.equalsIgnoreCase("foraging")) {
                    earnedXp += Double.parseDouble(addedXp) * 6.5;
                }
            }
        }
    }

    private final Color color = new Color(0, 200, 0, 80);
    private final Color targetColor = new Color(200, 0, 0, 80);

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!isRunning()) return;

        double x = mc.getRenderManager().viewerPosX;
        double y = mc.getRenderManager().viewerPosY;
        double z = mc.getRenderManager().viewerPosZ;
        if (currentTarget != null) {
            AxisAlignedBB bb = new AxisAlignedBB(currentTarget.xCoord - 0.05, currentTarget.yCoord - 0.05, currentTarget.zCoord - 0.05, currentTarget.xCoord + 0.05, currentTarget.yCoord + 0.05, currentTarget.zCoord + 0.05);
            bb = bb.offset(-x, -y, -z);
            RenderUtils.drawBox(bb, targetColor);
        }

        for (Vec3 dirtBlock : dirtBlocks) {
            if (dirtBlock == currentTarget) continue;
            AxisAlignedBB bb = new AxisAlignedBB(dirtBlock.xCoord - 0.05, dirtBlock.yCoord - 0.05, dirtBlock.zCoord - 0.05, dirtBlock.xCoord + 0.05, dirtBlock.yCoord + 0.05, dirtBlock.zCoord + 0.05);
            bb = bb.offset(-x, -y, -z);
            RenderUtils.drawBox(bb, color);
        }
    }

    private Vec3 getBestDirt() {
        Vec3 furthest = null;
        for (Vec3 pos : dirtBlocks) {
            Block block = mc.theWorld.getBlockState(new BlockPos(pos.xCoord, pos.yCoord + 0.1, pos.zCoord)).getBlock();
            pos = new Vec3(pos.xCoord + getBetween(-0.15f, 0.15f), pos.yCoord, pos.zCoord + getBetween(-0.15f, 0.15f));
            if (!(block instanceof net.minecraft.block.BlockLog) && block != Blocks.sapling) {
                if (furthest == null || mc.thePlayer.getDistance(pos.xCoord, pos.yCoord, pos.zCoord) > mc.thePlayer.getDistance(furthest.xCoord, furthest.yCoord, furthest.zCoord))
                    furthest = pos;
            }
        }
        return furthest;
    }

    private float getBetween(float min, float max) {
        return min + (new Random().nextFloat() * (max - min));
    }

    private List<Vec3> getDirts() {
        List<Vec3> dirtBlocks = new ArrayList<>();
        boolean leftSide = !BlockUtils.getRelativeBlock(-1, 0, 1).equals(Blocks.dirt);
        BlockPos frontLeftDirt;
        BlockPos frontRightDirt;
        BlockPos backLeftDirt;
        BlockPos backRightDirt;
        if (leftSide) {
            frontLeftDirt = BlockUtils.getRelativeBlockPos(0, 0, 2, AngleUtils.getClosest());
            frontRightDirt = BlockUtils.getRelativeBlockPos(1, 0, 2, AngleUtils.getClosest());
            backLeftDirt = BlockUtils.getRelativeBlockPos(0, 0, 1, AngleUtils.getClosest());
            backRightDirt = BlockUtils.getRelativeBlockPos(1, 0, 1, AngleUtils.getClosest());
        } else {
            frontLeftDirt = BlockUtils.getRelativeBlockPos(-1, 0, 2, AngleUtils.getClosest());
            frontRightDirt = BlockUtils.getRelativeBlockPos(0, 0, 2, AngleUtils.getClosest());
            backLeftDirt = BlockUtils.getRelativeBlockPos(-1, 0, 1, AngleUtils.getClosest());
            backRightDirt = BlockUtils.getRelativeBlockPos(0, 0, 1, AngleUtils.getClosest());
        }

        dirtBlocks.add(new Vec3(frontLeftDirt.getX() + 0.75, frontLeftDirt.getY() + 1, frontLeftDirt.getZ() + 0.75));
        dirtBlocks.add(new Vec3(frontRightDirt.getX() + 0.25, frontRightDirt.getY() + 1, frontRightDirt.getZ() + 0.75));
        dirtBlocks.add(new Vec3(backLeftDirt.getX() + 0.75, backLeftDirt.getY() + 1, backLeftDirt.getZ() + 0.25));
        dirtBlocks.add(new Vec3(backRightDirt.getX() + 0.25, backRightDirt.getY() + 1, backRightDirt.getZ() + 0.25));
        
        return dirtBlocks;
    }

    private void unstuck() {
        LogUtils.warn("[Foraging] I'm stuck! Unstuck process activated");
        stuck = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!isRunning()) {
            macroState = MacroState.LOOK;
            return;
        }

        if (RotationHandler.getInstance().isRotating()) return;

        if (stuck) {
            Vec3 closest = null;
            Vec3 player = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ);
            for (Vec3 dirt : dirtBlocks) {
                Block block = mc.theWorld.getBlockState(new BlockPos(dirt.xCoord, dirt.yCoord + 0.1, dirt.zCoord)).getBlock();
                BlockPos blockPos = new BlockPos(dirt.xCoord, dirt.yCoord + 0.1, dirt.zCoord);
                if ((block instanceof net.minecraft.block.BlockLog) || block == Blocks.sapling) {
                    Vec3 distance = new Vec3(blockPos.getX() + 0.5D + getBetween(-0.1f, 0.1f), (blockPos.getY() + 0.8 + getBetween(-0.1f, 0.1f)), blockPos.getZ() + 0.5D + getBetween(-0.1f, 0.1f));
                    if (closest == null || player.squareDistanceTo(distance) <= player.squareDistanceTo(closest))
                        closest = distance;
                }
            }
            int treecapitator = InventoryUtils.getSlotIdOfItemInHotbar("Treecapitator");
            if (treecapitator == -1) {
                LogUtils.error("No Treecapitator found in hotbar!");
                onDisable();
                return;
            }

            mc.thePlayer.inventory.currentItem = treecapitator;

            MovingObjectPosition mop = mc.objectMouseOver;

            boolean shouldBreak = mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !mc.theWorld.getBlockState(mop.getBlockPos()).getBlock().equals(Blocks.dirt);
            KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, shouldBreak);

            if (closest != null) {
                RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(closest), MayOBeesConfig.getRandomizedForagingMacroRotationSpeed(), RotationConfiguration.RotationType.CLIENT, null));
            } else {
                stuck = false;
                macroState = MacroState.LOOK;
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
                stuckTimer.schedule();
            }
            return;
        }

        if (stuckTimer.hasPassed(MayOBeesConfig.stuckTimeout)) {
            unstuck();
            return;
        }

        System.out.println(System.currentTimeMillis() - lastBreakTime);
        System.out.println((2000 - (MayOBeesConfig.monkeyLevel / 100f * 2000 * 0.5)));
        System.out.println("State: " + macroState);

        switch (macroState) {
            case LOOK:
                int saplingSlot = InventoryUtils.getSlotIdOfItemInHotbar("Sapling");
                if (saplingSlot == -1) {
                    LogUtils.error("No saplings found in hotbar!");
                    onDisable();
                    return;
                }
                for (Vec3 pos : dirtBlocks) {
                    Block block = mc.theWorld.getBlockState(new BlockPos(pos.xCoord, pos.yCoord + 0.1, pos.zCoord)).getBlock();
                    if (block instanceof BlockLog) {
                        unstuck();
                        return;
                    }
                }
                mc.thePlayer.inventory.currentItem = saplingSlot;
                currentTarget = getBestDirt();
                if (currentTarget != null) {
                    RotationHandler.getInstance().easeTo(new RotationConfiguration(new Target(currentTarget), (long) (MayOBeesConfig.getRandomizedForagingMacroRotationSpeed() * 1.5f), RotationConfiguration.RotationType.CLIENT, null));
                    macroState = MacroState.PLACE;
                } else {
                    macroState = MacroState.FIND_BONE;
                }
                return;
            case PLACE:
                KeyBindUtils.rightClick();
                macroState = MacroState.LOOK;
                return;
            case FIND_BONE:
                int boneMeal = InventoryUtils.getSlotIdOfItemInHotbar("Bone Meal");
                if (boneMeal == -1) {
                    LogUtils.error("No Bone Meal found in hotbar!");
                    onDisable();
                    return;
                }
                mc.thePlayer.inventory.currentItem = boneMeal;
                macroState = MacroState.PLACE_BONE;
                waitTimer.schedule();
                break;
            case PLACE_BONE:
                if (waitTimer.hasPassed(MayOBeesConfig.foragingDelay)) {
                    MovingObjectPosition mop = mc.objectMouseOver;
                    if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.theWorld.getBlockState(mop.getBlockPos()).getBlock().equals(Blocks.sapling)) {
                        KeyBindUtils.rightClick();
                    }
                    waitTimer.schedule();
                    if (MayOBeesConfig.foragingUseRod) {
                        macroState = MacroState.FIND_ROD;
                    } else {
                        macroState = MacroState.BREAK;
                    }
                }
                break;
            case FIND_ROD:
                if (waitTimer.hasPassed(MayOBeesConfig.foragingDelay)) {
                    int rod = InventoryUtils.getSlotIdOfItemInHotbar("Rod");
                    if (rod == -1) {
                        LogUtils.error("No Fishing Rod found in hotbar!");
                        onDisable();
                        break;
                    }
                    mc.thePlayer.inventory.currentItem = rod;
                    waitTimer.schedule();
                    macroState = MacroState.THROW_ROD;
                }
                break;
            case THROW_ROD:
                if (waitTimer.hasPassed(MayOBeesConfig.foragingDelay)) {
                    KeyBindUtils.rightClick();
                    waitTimer.schedule();
                    macroState = MacroState.THROW_BREAK_DELAY;
                }
                break;
            case THROW_BREAK_DELAY:
                if (waitTimer.hasPassed(MayOBeesConfig.foragingDelay)) {
                    waitTimer.schedule();
                    macroState = MacroState.BREAK;
                }
                break;
            case BREAK:
                int treecapitator = InventoryUtils.getSlotIdOfItemInHotbar("Treecapitator");
                if (treecapitator == -1) {
                    LogUtils.error("No Treecapitator found in hotbar!");
                    onDisable();
                    break;
                }
                mc.thePlayer.inventory.currentItem = treecapitator;
                if (lastBreakTime != 0 && System.currentTimeMillis() - lastBreakTime < (2000 - (MayOBeesConfig.monkeyLevel / 100f * 2000 * 0.5))) return;
                KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, true);
                waitAfterFinishTimer.schedule();
                macroState = MacroState.SWITCH;
                lastBreakTime = System.currentTimeMillis() + MayOBeesConfig.foragingMacroExtraBreakDelay;
                break;
            case SWITCH:
                if (waitAfterFinishTimer.hasPassed(150) && mc.gameSettings.keyBindAttack.isKeyDown()) {
                    System.out.println("Switching");
                    KeyBindUtils.setKeyBindState(mc.gameSettings.keyBindAttack, false);
                }
                if (waitAfterFinishTimer.hasPassed(350)) {
                    macroState = MacroState.LOOK;
                }
                break;
        }

        if (lastState != macroState) {
            lastState = macroState;
            stuckTimer.schedule();
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (!isRunning()) return;
        onDisable();
    }
}
