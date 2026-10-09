package com.avrgaming.civcraft.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Stairs;

/**
 * Fences, glass panes, iron bars, walls and stairs (corner shape): in 1.12 the connections were worked out when the block was drawn, in 1.13
 * they are stored in the block state. Blueprints only carry the old id:data, so every connection would stay off.
 * After a block is set (or removed) this class recomputes the connections of that block and of its four horizontal
 * neighbors, with the same rules the game uses when a player places them.
 */
public final class BlockConnections {

	private static final BlockFace[] SIDES = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

	private BlockConnections() {
	}

	private enum Kind { NONE, PANE, FENCE, NETHER_FENCE, WALL }

	private static Kind kindOf(Material m) {
		String n = m.name();
		if (n.endsWith("_PANE") || m == Material.IRON_BARS) {
			return Kind.PANE;
		}
		if (m == Material.NETHER_BRICK_FENCE) {
			return Kind.NETHER_FENCE;
		}
		if (n.endsWith("_FENCE")) {
			return Kind.FENCE;
		}
		if (n.endsWith("_WALL") && n.contains("COBBLESTONE")) {
			return Kind.WALL;
		}
		return Kind.NONE;
	}

	/** Call after the block at this position was changed: updates it and the connectable neighbors around it. */
	public static void update(Block block) {
		connect(block);
		for (BlockFace side : SIDES) {
			connect(block.getRelative(side));
		}
		// the other half of a door is above or below
		connect(block.getRelative(BlockFace.UP));
		connect(block.getRelative(BlockFace.DOWN));
	}

	/** The side a stair turns toward when you rotate its facing a quarter turn counter-clockwise (seen from above). */
	private static BlockFace rotateCcw(BlockFace face) {
		switch (face) {
		case NORTH:
			return BlockFace.WEST;
		case WEST:
			return BlockFace.SOUTH;
		case SOUTH:
			return BlockFace.EAST;
		default:
			return BlockFace.NORTH;
		}
	}

	private static boolean sameAxis(BlockFace a, BlockFace b) {
		return a == b || a == b.getOppositeFace();
	}

	/** A stair block next to this one with the same half (top or bottom), or null. */
	private static Stairs stairAt(Block block, Stairs self) {
		BlockData data = block.getBlockData();
		if (data instanceof Stairs && ((Stairs) data).getHalf() == self.getHalf()) {
			return (Stairs) data;
		}
		return null;
	}

	/** True when the block on that side is not a stair of exactly the same facing and half. */
	private static boolean isDifferentStair(Block block, Stairs self, BlockFace side) {
		BlockData data = block.getRelative(side).getBlockData();
		return !(data instanceof Stairs) || ((Stairs) data).getFacing() != self.getFacing()
				|| ((Stairs) data).getHalf() != self.getHalf();
	}

	/** The corner shape the game gives a stair placed here (same rules as the vanilla placement code). */
	private static Stairs.Shape stairShape(Block block, Stairs stair) {
		BlockFace facing = stair.getFacing();

		Stairs front = stairAt(block.getRelative(facing), stair);
		if (front != null && !sameAxis(front.getFacing(), facing)
				&& isDifferentStair(block, stair, front.getFacing().getOppositeFace())) {
			return front.getFacing() == rotateCcw(facing) ? Stairs.Shape.OUTER_LEFT : Stairs.Shape.OUTER_RIGHT;
		}

		Stairs back = stairAt(block.getRelative(facing.getOppositeFace()), stair);
		if (back != null && !sameAxis(back.getFacing(), facing) && isDifferentStair(block, stair, back.getFacing())) {
			return back.getFacing() == rotateCcw(facing) ? Stairs.Shape.INNER_LEFT : Stairs.Shape.INNER_RIGHT;
		}

		return Stairs.Shape.STRAIGHT;
	}

	private static void connectStair(Block block, Stairs stair) {
		Stairs.Shape shape = stairShape(block, stair);
		if (stair.getShape() != shape) {
			stair.setShape(shape);
			block.setBlockData(stair, false);
		}
	}

	/*
	 * Doors: 1.13 stores facing, open and hinge on both halves. The old data had facing and open on the lower half and
	 * the hinge on the upper half only, so each half comes out with the other half's values at their default. The
	 * lower half is the source for facing and open, the upper half for the hinge.
	 */
	private static void connectDoor(Block block, Door door) {
		boolean isUpper = door.getHalf() == Bisected.Half.TOP;
		Block lowerBlock = isUpper ? block.getRelative(BlockFace.DOWN) : block;
		Block upperBlock = isUpper ? block : block.getRelative(BlockFace.UP);
		BlockData lowerData = lowerBlock.getBlockData();
		BlockData upperData = upperBlock.getBlockData();
		if (!(lowerData instanceof Door) || !(upperData instanceof Door) || lowerData.getMaterial() != upperData.getMaterial()) {
			return;
		}
		Door lower = (Door) lowerData;
		Door upper = (Door) upperData;
		if (lower.getHalf() != Bisected.Half.BOTTOM || upper.getHalf() != Bisected.Half.TOP) {
			return;
		}

		if (lower.getHinge() != upper.getHinge()) {
			lower.setHinge(upper.getHinge());
			lowerBlock.setBlockData(lower, false);
		}
		if (upper.getFacing() != lower.getFacing() || upper.isOpen() != lower.isOpen()) {
			upper.setFacing(lower.getFacing());
			upper.setOpen(lower.isOpen());
			upperBlock.setBlockData(upper, false);
		}
	}

	/*
	 * Chests: two single chests side by side with the same facing become one double chest (type LEFT and RIGHT). The
	 * old data only had the facing, and the game joined them when it drew them. Rule from the vanilla placement code:
	 * the chest on the clockwise side of this chest's facing is its partner, and this chest is then the LEFT half.
	 */
	private static BlockFace rotateCw(BlockFace face) {
		switch (face) {
		case NORTH:
			return BlockFace.EAST;
		case EAST:
			return BlockFace.SOUTH;
		case SOUTH:
			return BlockFace.WEST;
		default:
			return BlockFace.NORTH;
		}
	}

	/** The chest at that block when it has the same material and facing and is free to pair (single, or already paired the other way). */
	private static Chest partnerAt(Block block, Chest chest, Chest.Type needed) {
		BlockData data = block.getBlockData();
		if (!(data instanceof Chest) || data.getMaterial() != chest.getMaterial()) {
			return null;
		}
		Chest other = (Chest) data;
		if (other.getFacing() != chest.getFacing()) {
			return null;
		}
		if (other.getType() != Chest.Type.SINGLE && other.getType() != needed) {
			return null;
		}
		return other;
	}

	private static void connectChest(Block block, Chest chest) {
		BlockFace facing = chest.getFacing();
		Block cwBlock = block.getRelative(rotateCw(facing));
		Block ccwBlock = block.getRelative(rotateCw(facing).getOppositeFace());

		Chest.Type type = Chest.Type.SINGLE;
		Chest cw = partnerAt(cwBlock, chest, Chest.Type.RIGHT);
		Chest ccw = partnerAt(ccwBlock, chest, Chest.Type.LEFT);
		if (cw != null) {
			type = Chest.Type.LEFT;
			if (cw.getType() != Chest.Type.RIGHT) {
				cw.setType(Chest.Type.RIGHT);
				cwBlock.setBlockData(cw, false);
			}
		} else if (ccw != null) {
			type = Chest.Type.RIGHT;
			if (ccw.getType() != Chest.Type.LEFT) {
				ccw.setType(Chest.Type.LEFT);
				ccwBlock.setBlockData(ccw, false);
			}
		}

		if (chest.getType() != type) {
			chest.setType(type);
			block.setBlockData(chest, false);
		}
	}

	private static void connect(Block block) {
		BlockData current = block.getBlockData();
		if (current instanceof Stairs) {
			connectStair(block, (Stairs) current);
			return;
		}
		if (current instanceof Door) {
			connectDoor(block, (Door) current);
			return;
		}
		if (current instanceof Chest) {
			connectChest(block, (Chest) current);
			return;
		}
		Kind kind = kindOf(block.getType());
		if (kind == Kind.NONE) {
			return;
		}
		BlockData data = block.getBlockData();
		if (!(data instanceof MultipleFacing)) {
			return;
		}
		MultipleFacing facing = (MultipleFacing) data;
		boolean changed = false;
		for (BlockFace side : SIDES) {
			if (!facing.getAllowedFaces().contains(side)) {
				continue;
			}
			boolean connects = connectsTo(kind, block.getRelative(side));
			if (facing.hasFace(side) != connects) {
				facing.setFace(side, connects);
				changed = true;
			}
		}
		if (changed) {
			block.setBlockData(facing, false);
		}
	}

	private static boolean connectsTo(Kind kind, Block other) {
		Material m = other.getType();
		Kind otherKind = kindOf(m);
		switch (kind) {
		case PANE:
			if (otherKind == Kind.PANE) {
				return true;
			}
			return isSolidWall(m);
		case FENCE:
			return otherKind == Kind.FENCE || m.name().endsWith("_FENCE_GATE") || isSolidWall(m);
		case NETHER_FENCE:
			return otherKind == Kind.NETHER_FENCE || m.name().endsWith("_FENCE_GATE") || isSolidWall(m);
		case WALL:
			return otherKind == Kind.WALL || m.name().endsWith("_FENCE_GATE") || isSolidWall(m);
		default:
			return false;
		}
	}

	/** A full opaque block the thin blocks attach to (not glass, leaves or other see-through cubes). */
	private static boolean isSolidWall(Material m) {
		if (!m.isOccluding()) {
			return false;
		}
		String n = m.name();
		return !n.contains("GLASS") && !n.contains("LEAVES") && !n.contains("SHULKER_BOX") && m != Material.BEACON
				&& m != Material.PUMPKIN && m != Material.MELON && m != Material.BARRIER && m != Material.GLOWSTONE;
	}
}
