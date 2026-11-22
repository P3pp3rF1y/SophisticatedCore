package net.p3pp3rf1y.sophisticatedcore.api;

public class InventoryLayout {
	private InventoryLayout() {
	}

	public static int getBaseColumns(int inventorySlots) {
		return inventorySlots <= 81 ? 9 : 12;
	}

	public static int getEffectiveColumns(int inventorySlots, int columnsTaken) {
		return getBaseColumns(inventorySlots) - columnsTaken;
	}

	public static int getNumberOfRows(int inventorySlots) {
		return (int) Math.ceil((double) inventorySlots / getBaseColumns(inventorySlots));
	}

	public static int getUsableInventorySlots(int inventorySlots, int columnsTaken) {
		return inventorySlots - getNumberOfRows(inventorySlots) * columnsTaken;
	}
}
