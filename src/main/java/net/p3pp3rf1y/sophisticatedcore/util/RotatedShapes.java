package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RotatedShapes {
	private final VoxelShape[] rotatedShapes;
	private final boolean horizontal;

	public RotatedShapes(VoxelShape... shapes) {
		this(true, shapes);
	}

	public RotatedShapes(boolean horizontal, VoxelShape... shapes) {
		rotatedShapes = new VoxelShape[horizontal ? 4 : 6];
		this.horizontal = horizontal;
		rotatedShapes[0] = or(Stream.of(shapes));
	}

	public VoxelShape getRotatedShape(Direction to) {
		int index = horizontal ? (to.get2DDataValue() + 4) % 4 : to.get3DDataValue();
		if (rotatedShapes[index] == null) {
			if (horizontal) {
				rotatedShapes[index] = rotateHorizontal(to);
			} else {
				rotatedShapes[index] = rotate(to);
			}
		}
		return rotatedShapes[index];
	}

	private VoxelShape rotateHorizontal(Direction dir) {
		return switch (dir) {
			case NORTH -> rotateShape(rotatedShapes[0], (minX, minY, minZ, maxX, maxY, maxZ) -> box(1 - minX, minY, 1 - minZ, 1 - maxX, maxY, 1 - maxZ));
			case SOUTH -> rotatedShapes[0];
			case WEST -> rotateShape(rotatedShapes[0], (minX, minY, minZ, maxX, maxY, maxZ) -> box(1 - minZ, minY, minX, 1 - maxZ, maxY, maxX));
			case EAST -> rotateShape(rotatedShapes[0], (minX, minY, minZ, maxX, maxY, maxZ) -> box(minZ, minY, 1 - minX, maxZ, maxY, 1 - maxX));
			default -> rotatedShapes[0];
		};
	}

	private VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return Shapes.box(Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ), Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ));
	}

	private VoxelShape rotate(Direction dir) {
		return switch (dir) {
			case DOWN -> rotatedShapes[0];
			case UP -> rotateShape(rotatedShapes[0], (minX, minY, minZ, maxX, maxY, maxZ) -> box(minX, 1 - minY, 1 - minZ, maxX, 1 - maxY, 1 - maxZ));
			case NORTH -> rotateShape(rotatedShapes[0], (minX, minY, minZ, maxX, maxY, maxZ) -> box(minX, 1 - minZ, minY, maxX, 1 - maxZ, maxY));
			case SOUTH -> rotateShape(rotatedShapes[0], (minX, minY, minZ, maxX, maxY, maxZ) -> box(1 - minX, 1 - minZ, 1 - minY, 1 - maxX, 1 - maxZ, 1 - maxY));
			case WEST -> //noinspection SuspiciousNameCombination - nothing suspicious here given that we're rotating
					rotateShape(rotatedShapes[0], (minX, minY, minZ, maxX, maxY, maxZ) -> box(minY, 1 - minZ, 1 - minX, maxY, 1 - maxZ, 1 - maxX));
			case EAST -> rotateShape(rotatedShapes[0], (minX, minY, minZ, maxX, maxY, maxZ) -> box(1 - minY, 1 - minZ, minX, 1 - maxY, 1 - maxZ, maxX));
		};
	}

	private VoxelShape rotateShape(VoxelShape shape, DoubleLineFunction rotate) {
		List<VoxelShape> shapes = new ArrayList<>();
		shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> shapes.add(rotate.apply(minX, minY, minZ, maxX, maxY, maxZ)));
		return or(shapes.stream());
	}

	private VoxelShape or(Stream<VoxelShape> shapes) {
		return shapes.reduce((v1, v2) -> Shapes.joinUnoptimized(v1, v2, BooleanOp.OR))
				.map(VoxelShape::optimize)
				.orElse(Shapes.empty());
	}

	public interface DoubleLineFunction {
		VoxelShape apply(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
	}
}
