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
    public String validateCraft(Craft craft) {

        BlockFace cruiseFace = null;
        List<Integer> engineBlockIds = getMoveBlocks(craft.getType());
        Set<Location> engineBlocks = new HashSet<Location>();

        for (MovecraftLocation ml : craft.getHitBox()) {

            Location location = ml.toBukkit(craft.getW());
            Block block = location.getBlock();
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
        craft.getNotificationPlayer().sendMessage(cruiseFace.getModX() + ", " + cruiseFace.getModZ());

        Function<Location, Boolean> inHitbox = null;
        if (vector.getBlockX() > 0)
            inHitbox = (l) -> {
                return l.getBlockX() <= craft.getHitBox().getMaxX();
            };
        else if (vector.getBlockX() < 0)
            inHitbox = (l) -> {
                return l.getBlockX() >= craft.getHitBox().getMinX();
            };
        else if (vector.getBlockZ() > 0)
            inHitbox = (l) -> {
                return l.getBlockZ() <= craft.getHitBox().getMaxZ();
            };
        else if (vector.getBlockZ() < 0)
            inHitbox = (l) -> {
                return l.getBlockZ() >= craft.getHitBox().getMinZ();
            };

        Set<Location> checkedEngineBlocks = new HashSet<Location>();
        for (Location l : engineBlocks) {
            if (checkedEngineBlocks.contains(l))
                continue;

            Location location = l.clone().add(vector);
            while (inHitbox.apply(location)) {

                Block block = location.getBlock();
                if (engineBlockIds.contains(block.getTypeId()))
                    checkedEngineBlocks.add(location);
                else if (!(block.getType() == Material.AIR || isVenting(block.getTypeId())))
                    return "engineBlocked";
                location.add(vector);

            }
        }

        return "valid";

    }

    public List<Integer> getMoveBlocks(CraftType type) {
        List<Integer> typeIds = new ArrayList<Integer>();
        for (List<Integer> list : type.getMoveBlocks().keySet())
            typeIds.addAll(list);
        return typeIds;
    }

    public boolean isVenting(int typeId) {
        return CraftValidator.getInstance().getConfig().getIntegerList("ventingBlocks").contains(typeId);
    }

    public boolean isMoveBlock(CraftType type, int typeId) {
        for (List<Integer> list : type.getMoveBlocks().keySet()) {
            if (list.contains(typeId))
                return true;
        }
        return false;
    }

}
