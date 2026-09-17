package net.p3pp3rf1y.sophisticatedcore.linkedstorage;

import net.minecraft.network.chat.Component;

public record LinkedStorageSnapshotProfile(Component groupName, int inventorySlots, int upgradeSlots, int columnsTaken) {
}
