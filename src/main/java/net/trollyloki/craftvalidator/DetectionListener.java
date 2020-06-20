package net.trollyloki.craftvalidator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.material.Stairs;
import org.bukkit.util.Vector;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;

public class DetectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraftDetect(CraftDetectEvent event) {

        String validation = validateCraft(event.getCraft());
        if (!validation.equals("valid")) {
            event.setFailMessage(validation);
            event.setCancelled(true);
        }

    }

    @SuppressWarnings("deprecation")
    public static String validateCraft(Craft craft) {

        List<Integer> engineBlockIds = getMoveBlocks(craft.getType());
        BlockFace cruiseFace = null;
        Set<MovecraftLocation> engineBlocks = new HashSet<MovecraftLocation>();

        for (MovecraftLocation location : craft.getHitBox()) {

            Block block = location.toBukkit(craft.getW()).getBlock();
            BlockState state = block.getState();
            if (state instanceof Sign) {
                Sign sign = (Sign) state;
                if (sign.getLine(0).equalsIgnoreCase("Cruise: OFF") || sign.getLine(0).equalsIgnoreCase("Cruise: ON")) {

                    BlockFace face = ((org.bukkit.material.Sign) sign.getData()).getFacing();
                    if (cruiseFace == null)
                        cruiseFace = face;
                    else if (face != cruiseFace)
                        return "multipleCruiseDirections";

                }
            }

            if (engineBlockIds.contains(block.getTypeId()))
                engineBlocks.add(location);

        }

        if (cruiseFace == null)
            return "noCruiseSign";
        Vector vector = new Vector(cruiseFace.getModX(), 0, cruiseFace.getModZ());
        if (!validateCruiseDirection(craft, vector))
            return "invalidCruiseDirection";

        int invalidEngines = validateEngines(craft, engineBlocks, vector);
        if (invalidEngines > 0)
            return "engineBlocked - " + invalidEngines + " more than allowed blocked";

        return "valid";

    }

    public static boolean validateCruiseDirection(Craft craft, Vector vector) {

        if (vector.getBlockX() != 0) {
            return craft.getHitBox().getXLength() > craft.getHitBox().getZLength();
        }

        else if (vector.getBlockZ() != 0) {
            return craft.getHitBox().getZLength() > craft.getHitBox().getXLength();
        }

        return false;

    }

    public static int validateEngines(Craft craft, Set<MovecraftLocation> engineBlocks, Vector vector) {

        Function<MovecraftLocation, Boolean> inHitbox = null;
        if (vector.getBlockX() > 0)
            inHitbox = (l) -> {
                return l.getX() <= craft.getHitBox().getMaxX();
            };
        else if (vector.getBlockX() < 0)
            inHitbox = (l) -> {
                return l.getX() >= craft.getHitBox().getMinX();
            };
        else if (vector.getBlockZ() > 0)
            inHitbox = (l) -> {
                return l.getZ() <= craft.getHitBox().getMaxZ();
            };
        else if (vector.getBlockZ() < 0)
            inHitbox = (l) -> {
                return l.getZ() >= craft.getHitBox().getMinZ();
            };

        int maxInvalidEngineBlocks = (int) (craft.getHitBox().size()
                * CraftValidator.getInstance().getConfig().getDouble("maxInvalidEnginesPercent") * 0.01);
        int invalidEngineBlocks = 0;
        for (MovecraftLocation location : engineBlocks) {
            if (engineBlocks.contains(location.translate(-vector.getBlockX(), 0, -vector.getBlockZ())))
                continue;

            boolean ventingFound = false;
            int engineBlocksCount = 1;
            location = location.translate(vector.getBlockX(), 0, vector.getBlockZ());
            while (inHitbox.apply(location)) {

                Block block = location.toBukkit(craft.getW()).getBlock();
                if (!ventingFound && engineBlocks.contains(location)) {
                    engineBlocksCount++;
                }

                else if (block.getType() == Material.AIR || (!ventingFound && isVentingBlock(block, vector)))
                    ventingFound = true;

                else {
                    invalidEngineBlocks += engineBlocksCount;
                    // if (invalidEngineBlocks > maxInvalidEngineBlocks) // end
                    break;
                }

                location = location.translate(vector.getBlockX(), 0, vector.getBlockZ());

            }
        }

        return invalidEngineBlocks - maxInvalidEngineBlocks;

    }

    public static List<Integer> getMoveBlocks(CraftType type) {
        List<Integer> typeIds = new ArrayList<Integer>();
        for (List<Integer> list : type.getMoveBlocks().keySet())
            typeIds.addAll(list);
        return typeIds;
    }

    public static boolean isVentingTypeId(int typeId) {
        return CraftValidator.getInstance().getConfig().getIntegerList("ventingBlocks").contains(typeId);
    }

    @SuppressWarnings("deprecation")
    public static boolean isVentingBlock(Block block, Vector vector) {
        if (isVentingTypeId(block.getTypeId())) {
            if (block.getState().getData() instanceof Stairs) {
                if (!isVentingStair(block, vector))
                    return false;
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static boolean isVentingStair(Block block, Vector vector) {
        Block testBlock = null;
        byte[] testData = null;

        if (vector.getBlockX() != 0) {

            if (block.getData() == 2) {
                testBlock = block.getRelative(0, 0, -1);
                testData = new byte[] { 0, 1 };
            } else if (block.getData() == 3) {
                testBlock = block.getRelative(0, 0, 1);
                testData = new byte[] { 0, 1 };
            } else if (block.getData() == 6) {
                testBlock = block.getRelative(0, 0, -1);
                testData = new byte[] { 4, 5 };
            } else if (block.getData() == 7) {
                testBlock = block.getRelative(0, 0, 1);
                testData = new byte[] { 4, 5 };
            }

        }

        else if (vector.getBlockZ() != 0) {

            if (block.getData() == 0) {
                testBlock = block.getRelative(-1, 0, 0);
                testData = new byte[] { 2, 3 };
            } else if (block.getData() == 1) {
                testBlock = block.getRelative(1, 0, 0);
                testData = new byte[] { 2, 3 };
            } else if (block.getData() == 4) {
                testBlock = block.getRelative(-1, 0, 0);
                testData = new byte[] { 6, 7 };
            } else if (block.getData() == 5) {
                testBlock = block.getRelative(1, 0, 0);
                testData = new byte[] { 6, 7 };
            }

        }

        if (testBlock != null) {
            if (!(testBlock.getState().getData() instanceof Stairs))
                return true;
            for (byte test : testData) {
                if (testBlock.getData() == test)
                    return false;
            }
            return true;
        }

        return false;
    }

    public static boolean isMoveBlock(CraftType type, int typeId) {
        for (List<Integer> list : type.getMoveBlocks().keySet()) {
            if (list.contains(typeId))
                return true;
        }
        return false;
    }

    public static MovecraftLocation mLocFromBukkit(Location location) {
        return new MovecraftLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

}
