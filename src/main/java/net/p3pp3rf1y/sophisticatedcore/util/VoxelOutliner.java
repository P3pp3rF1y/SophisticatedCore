package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

public final class VoxelOutliner {

	private VoxelOutliner() {
	}

	private static final int MASK_EAST = 1 << 0;
	private static final int MASK_WEST = 1 << 1;
	private static final int MASK_UP = 1 << 2;
	private static final int MASK_DOWN = 1 << 3;
	private static final int MASK_SOUTH = 1 << 4;
	private static final int MASK_NORTH = 1 << 5;

	private static int maskOf(Collection<Direction> dirs) {
		int m = 0;
		for (Direction d : dirs) {
			switch (d) {
				case EAST -> m |= MASK_EAST;
				case WEST -> m |= MASK_WEST;
				case UP -> m |= MASK_UP;
				case DOWN -> m |= MASK_DOWN;
				case SOUTH -> m |= MASK_SOUTH;
				case NORTH -> m |= MASK_NORTH;
			}
		}
		return m;
	}

	public static final class Edge {
		private final Vec3 a;
		private final Vec3 b;
		private final Axis axis;
		private final double length;

		public Edge(int x1, int y1, int z1, int x2, int y2, int z2) {
			this(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2));
		}

		public Edge(Vec3 a, Vec3 b) {
			this.a = a;
			this.b = b;
			if (a.x != b.x) {
				axis = Axis.X;
			} else if (a.y != b.y) {
				axis = Axis.Y;
			} else {
				axis = Axis.Z;
			}
			this.length = a.distanceTo(b);
		}

		public Vec3 a() {
			return a;
		}

		public Vec3 b() {
			return b;
		}

		public Axis axis() {
			return axis;
		}

		public double length() {
			return length;
		}
	}

	public static List<Edge> computeShapeRenderableEdges(Level level, List<BlockPos> positions) {
		List<Edge> edges = new ArrayList<>();

		positions.forEach(pos -> {
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getShape(level, pos);
			if (state.getBlock() instanceof IDoubleBlock doubleBlock) {
				VoxelShape finalShape = shape;
				shape = doubleBlock.getOtherPosition(state, pos).map(otherPos -> {
					BlockState otherState = level.getBlockState(otherPos);
					VoxelShape otherShape = otherState.getShape(level, otherPos);
					otherShape = otherShape.move(otherPos.getX() - pos.getX(), otherPos.getY() - pos.getY(), otherPos.getZ() - pos.getZ());
					return Shapes.join(finalShape, otherShape, BooleanOp.OR);
				}).orElse(shape);
			}
			edges.addAll(linesFromVoxelShapeSimplified(shape, pos));
		});

		return edges;
	}

	public static List<Edge> linesFromVoxelShapeSimplified(VoxelShape shape, BlockPos pos) {
		if (shape.isEmpty()) return List.of();

		List<Edge> edges = new ArrayList<>();
		shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
			Vec3 a = new Vec3(minX, minY, minZ).add(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
			Vec3 b = new Vec3(maxX, maxY, maxZ).add(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
			edges.add(new Edge(a, b));
		});

		if (edges.size() <= 12) {
			return edges;
		}

		return boundsEdgesFromShape(shape, pos);
	}

	private static double f1(Edge e) {
		return switch (e.axis()) {
			case X -> e.a().y;
			case Y -> e.a().x;
			case Z -> e.a().x;
		};
	}

	private static double f2(Edge e) {
		return switch (e.axis()) {
			case X -> e.a().z;
			case Y -> e.a().z;
			case Z -> e.a().y;
		};
	}

	private static boolean isSilhouette(Edge e, double[] b, double eps) {
		double f1 = f1(e), f2 = f2(e);
		return nearly(f1, b[0], eps) || nearly(f1, b[1], eps) || nearly(f2, b[2], eps) || nearly(f2, b[3], eps);
	}

	private static boolean nearly(double a, double b, double eps) {
		return Math.abs(a - b) <= eps;
	}


	public static List<Edge> computeRenderableEdges(Collection<BlockPos> blocks) {
		HashSet<BlockPos> solid = new HashSet<>(blocks);
		NormalsAccumulator acc = new NormalsAccumulator();

		for (BlockPos p : solid) {
			int x = p.getX(), y = p.getY(), z = p.getZ();
			for (Direction dir : Direction.values()) {
				if (solid.contains(p.relative(dir))) continue; // hidden face

				switch (dir.getAxis()) {
					case X -> {
						int xf = dir == Direction.EAST ? x + 1 : x;
						acc.addFaceEdges(dir,
								new EdgeKey(xf, y, z, xf, y + 1, z),
								new EdgeKey(xf, y + 1, z, xf, y + 1, z + 1),
								new EdgeKey(xf, y + 1, z + 1, xf, y, z + 1),
								new EdgeKey(xf, y, z + 1, xf, y, z));
					}
					case Y -> {
						int yf = dir == Direction.UP ? y + 1 : y;
						acc.addFaceEdges(dir,
								new EdgeKey(x, yf, z, x + 1, yf, z),
								new EdgeKey(x + 1, yf, z, x + 1, yf, z + 1),
								new EdgeKey(x + 1, yf, z + 1, x, yf, z + 1),
								new EdgeKey(x, yf, z + 1, x, yf, z));
					}
					case Z -> {
						int zf = dir == Direction.SOUTH ? z + 1 : z;
						acc.addFaceEdges(dir,
								new EdgeKey(x, y, zf, x + 1, y, zf),
								new EdgeKey(x + 1, y, zf, x + 1, y + 1, zf),
								new EdgeKey(x + 1, y + 1, zf, x, y + 1, zf),
								new EdgeKey(x, y + 1, zf, x, y, zf));
					}
				}
			}
		}

		List<EdgeInt> edges = new ArrayList<>();
		for (var e : acc.edgeToNormals.entrySet()) {
			EnumMap<Direction, Integer> normals = e.getValue();
			int total = normals.values().stream().mapToInt(i -> i).sum();
			if (normals.size() == 1 && total == 2) {
				continue; // internal planar seam
			}
			int edgeMask = maskOf(normals.keySet());
			EdgeKey k = e.getKey();
			edges.add(new EdgeInt(k.x1(),k.y1(),k.z1(), k.x2(),k.y2(),k.z2(), k.axis(), edgeMask));
		}

		edges = mergeColinearEdges(edges);

		List<Edge> out = new ArrayList<>(edges.size());
		for (EdgeInt e : edges) {
			Vec3 a = new Vec3(e.x1, e.y1, e.z1);
			Vec3 b = new Vec3(e.x2, e.y2, e.z2);

			out.add(new Edge(a, b));
		}
		return out;
	}

	private static List<EdgeInt> mergeColinearEdges(List<EdgeInt> edges) {
		record GKey(Axis axis, int a, int b, int mask) {}
		Map<GKey, List<EdgeInt>> groups = new HashMap<>();
		for (EdgeInt e : edges) {
			switch (e.axis) {
				case X -> groups.computeIfAbsent(new GKey(Axis.X, e.y1, e.z1, e.edgeMask), k -> new ArrayList<>()).add(e);
				case Y -> groups.computeIfAbsent(new GKey(Axis.Y, e.x1, e.z1, e.edgeMask), k -> new ArrayList<>()).add(e);
				case Z -> groups.computeIfAbsent(new GKey(Axis.Z, e.x1, e.y1, e.edgeMask), k -> new ArrayList<>()).add(e);
			}
		}

		List<EdgeInt> merged = new ArrayList<>(edges.size());
		for (var ent : groups.entrySet()) {
			List<EdgeInt> list = ent.getValue();
			if (list.isEmpty()) continue;
			Axis axis = ent.getKey().axis();
			int mask = ent.getKey().mask();

			switch (axis) {
				case X -> {
					list.sort(Comparator.comparingInt(e -> e.x1));
					int y = list.get(0).y1, z = list.get(0).z1;
					int s = list.get(0).x1, epos = list.get(0).x2;
					for (int i = 1; i < list.size(); i++) {
						EdgeInt e = list.get(i);
						if (e.y1 == y && e.z1 == z && e.x1 == epos) epos = e.x2;
						else { merged.add(new EdgeInt(s,y,z, epos,y,z, Axis.X, mask)); y = e.y1; z = e.z1; s = e.x1; epos = e.x2; }
					}
					merged.add(new EdgeInt(s,y,z, epos,y,z, Axis.X, mask));
				}
				case Y -> {
					list.sort(Comparator.comparingInt(e -> e.y1));
					int x = list.get(0).x1, z = list.get(0).z1;
					int s = list.get(0).y1, epos = list.get(0).y2;
					for (int i = 1; i < list.size(); i++) {
						EdgeInt e = list.get(i);
						if (e.x1 == x && e.z1 == z && e.y1 == epos) epos = e.y2;
						else { merged.add(new EdgeInt(x,s,z, x,epos,z, Axis.Y, mask)); x = e.x1; z = e.z1; s = e.y1; epos = e.y2; }
					}
					merged.add(new EdgeInt(x,s,z, x,epos,z, Axis.Y, mask));
				}
				case Z -> {
					list.sort(Comparator.comparingInt(e -> e.z1));
					int x = list.get(0).x1, y = list.get(0).y1;
					int s = list.get(0).z1, epos = list.get(0).z2;
					for (int i = 1; i < list.size(); i++) {
						EdgeInt e = list.get(i);
						if (e.x1 == x && e.y1 == y && e.z1 == epos) epos = e.z2;
						else { merged.add(new EdgeInt(x,y,s, x,y,epos, Axis.Z, mask)); x = e.x1; y = e.y1; s = e.z1; epos = e.z2; }
					}
					merged.add(new EdgeInt(x,y,s, x,y,epos, Axis.Z, mask));
				}
			}
		}
		return merged;
	}

	private record EdgeKey(int x1, int y1, int z1, int x2, int y2, int z2) {
		EdgeKey {
			if ((x1 > x2) || (x1 == x2 && (y1 > y2 || (y1 == y2 && z1 > z2)))) {
				int tx=x1, ty=y1, tz=z1; x1=x2; y1=y2; z1=z2; x2=tx; y2=ty; z2=tz;
			}
		}
		Axis axis() {
			if (x1 != x2) return Axis.X;
			if (y1 != y2) return Axis.Y;
			if (z1 != z2) return Axis.Z;
			throw new IllegalStateException("Non-axis aligned edge");
		}
	}

	private static final class NormalsAccumulator {
		private final Map<EdgeKey, EnumMap<Direction, Integer>> edgeToNormals = new HashMap<>();
		void addFaceEdges(Direction faceNormal, EdgeKey... keys) {
			for (EdgeKey k : keys) {
				EnumMap<Direction, Integer> m = edgeToNormals.computeIfAbsent(k, kk -> new EnumMap<>(Direction.class));
				m.merge(faceNormal, 1, Integer::sum);
			}
		}
	}

	private record EdgeInt(int x1, int y1, int z1, int x2, int y2, int z2, Axis axis, int edgeMask) {
	}

	private static List<Edge> boundsEdgesFromShape(VoxelShape shape, BlockPos pos) {
		return boxEdges(shape.min(Axis.X) + pos.getX(), shape.min(Axis.Y) + pos.getY(), shape.min(Axis.Z) + pos.getZ(),
				shape.max(Axis.X) + pos.getX(), shape.max(Axis.Y) + pos.getY(), shape.max(Axis.Z) + pos.getZ());
	}

	private static List<Edge> boxEdges(double x0, double y0, double z0,
									   double x1, double y1, double z1) {
		if (x1 <= x0 || y1 <= y0 || z1 <= z0) return List.of();

		var out = new ArrayList<Edge>(12);

		// bottom (y0)
		out.add(new Edge(new Vec3(x0,y0,z0), new Vec3(x1,y0,z0)));
		out.add(new Edge(new Vec3(x1,y0,z0), new Vec3(x1,y0,z1)));
		out.add(new Edge(new Vec3(x1,y0,z1), new Vec3(x0,y0,z1)));
		out.add(new Edge(new Vec3(x0,y0,z1), new Vec3(x0,y0,z0)));

		// top (y1)
		out.add(new Edge(new Vec3(x0,y1,z0), new Vec3(x1,y1,z0)));
		out.add(new Edge(new Vec3(x1,y1,z0), new Vec3(x1,y1,z1)));
		out.add(new Edge(new Vec3(x1,y1,z1), new Vec3(x0,y1,z1)));
		out.add(new Edge(new Vec3(x0,y1,z1), new Vec3(x0,y1,z0)));

		// verticals
		out.add(new Edge(new Vec3(x0,y0,z0), new Vec3(x0,y1,z0)));
		out.add(new Edge(new Vec3(x1,y0,z0), new Vec3(x1,y1,z0)));
		out.add(new Edge(new Vec3(x1,y0,z1), new Vec3(x1,y1,z1)));
		out.add(new Edge(new Vec3(x0,y0,z1), new Vec3(x0,y1,z1)));

		return out;
	}

	public static List<Edge> edgesFromAABB(AABB aabb) {
		return boxEdges(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
	}
}
