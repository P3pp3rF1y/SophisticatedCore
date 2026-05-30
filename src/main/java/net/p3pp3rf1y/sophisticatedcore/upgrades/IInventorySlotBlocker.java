package net.p3pp3rf1y.sophisticatedcore.upgrades;

public interface IInventorySlotBlocker {
	boolean isSlotBlocked(int slot);

	default boolean shouldRenderBlockedSlotOverlay(int slot) {
		return false;
	}
}
