package net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.rei;

import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import net.minecraft.world.phys.AABB;

public abstract class ReiGhostTarget implements DraggableStackVisitor.BoundsProvider {

	public abstract void accept();

	public boolean contains(int x, int y) {
		AABB box = bounds().bounds();
		return box.contains(x, y, box.minZ);
	}
}
