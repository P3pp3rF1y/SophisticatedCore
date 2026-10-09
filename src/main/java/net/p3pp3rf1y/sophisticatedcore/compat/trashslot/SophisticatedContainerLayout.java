package net.p3pp3rf1y.sophisticatedcore.compat.trashslot;

import net.blay09.mods.balm.mixin.AbstractContainerScreenAccessor;
import net.blay09.mods.trashslot.api.layout.*;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SophisticatedContainerLayout implements TrashContainerLayout {
	public static final SophisticatedContainerLayout INSTANCE = new SophisticatedContainerLayout();
	public static final int PLAYER_INVENTORY_WIDTH = 7 + 18 * 9 + 7;
	public static final int HEIGHT_OF_PLAYER_INVENTORY_STICKING_OUT = 7 + 18 + 4 + 18 * 3;
	private static final Identifier MAIN_BOUNDS_ID = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/main");
	private static final Identifier PLAYER_INVENTORY_BOUNDS_ID = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/player_inventory");
	private static final Identifier UPGRADE_SLOTS_BOUNDS_ID = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/upgrade_slots");
	private static final Identifier SORT_BUTTONS_BOUNDS_ID = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/sort_buttons");
	private static final Identifier PLAYER_BOTTOM_RIGHT_SNAP_ID = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/player_bottom_right");
	private static final Identifier PLAYER_RIGHT_BOTTOM_SNAP_ID = Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/player_right_bottom");
	private static final Map<Identifier, Snap> SNAPS = createSnaps();

	private SophisticatedContainerLayout() {
	}

	@Override
	public List<Rect2i> getAllBounds(TrashSlotContainerContext context) {
		if (context.screen() instanceof StorageScreenBase<?> storageScreen) {
			List<Rect2i> collisionAreas = new ArrayList<>();
			AbstractContainerScreenAccessor screenAccessor = (AbstractContainerScreenAccessor) context.screen();
			collisionAreas.add(getMainBounds(storageScreen, screenAccessor));
			getPlayerInventoryBounds(storageScreen, screenAccessor).ifPresent(collisionAreas::add);
			storageScreen.getUpgradeSlotsRectangle().ifPresent(collisionAreas::add);
			collisionAreas.addAll(storageScreen.getUpgradeSettingsControl().getTabRectangles());
			storageScreen.getSortButtonsRectangle().ifPresent(collisionAreas::add);
			return collisionAreas;
		}

		return List.of(ScreenBoundsProvider.SCREEN.get(context));
	}

	@Override
	public Optional<Rect2i> getBounds(TrashSlotContainerContext context, Identifier identifier) {
		AbstractContainerScreenAccessor screenAccessor = (AbstractContainerScreenAccessor) context.screen();
		if (identifier.equals(ScreenBoundsProvider.SCREEN_ID)) {
			return Optional.of(ScreenBoundsProvider.SCREEN.get(context));
		}

		if (!(context.screen() instanceof StorageScreenBase<?> storageScreen)) {
			return Optional.empty();
		}

		if (identifier.equals(MAIN_BOUNDS_ID)) {
			return Optional.of(getMainBounds(storageScreen, screenAccessor));
		}
		if (identifier.equals(PLAYER_INVENTORY_BOUNDS_ID)) {
			return getPlayerInventoryBounds(storageScreen, screenAccessor);
		}
		if (identifier.equals(UPGRADE_SLOTS_BOUNDS_ID)) {
			return storageScreen.getUpgradeSlotsRectangle();
		}
		if (identifier.equals(SORT_BUTTONS_BOUNDS_ID)) {
			return storageScreen.getSortButtonsRectangle();
		}

		return Optional.empty();
	}

	@Override
	public Optional<Snap> getSnap(TrashSlotContainerContext context, Identifier identifier) {
		return Optional.ofNullable(SNAPS.get(identifier));
	}

	@Override
	public Map<Identifier, Snap> getSnaps(TrashSlotContainerContext context) {
		return SNAPS;
	}

	@Override
	public Optional<Snap> getDefaultSnap(TrashSlotContainerContext context) {
		AbstractContainerScreenAccessor screenAccessor = (AbstractContainerScreenAccessor) context.screen();
		return getSnap(context,
				context.screen().height - screenAccessor.getImageHeight() > 2 * SlotVisual.DEFAULT.getHeight()
						? PLAYER_BOTTOM_RIGHT_SNAP_ID
						: PLAYER_RIGHT_BOTTOM_SNAP_ID);
	}

	@Override
	public TrashSlotAvailability getAvailability() {
		return TrashSlotAvailability.DEFAULT;
	}

	private static Map<Identifier, Snap> createSnaps() {
		Map<Identifier, Snap> snaps = new LinkedHashMap<>();
		snaps.put(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/top"),
				edgeSnap(ScreenBoundsProvider.SCREEN_ID, SlotVisual.ATTACH_TOP, true));
		snaps.put(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/left"), edgeSnap(MAIN_BOUNDS_ID, SlotVisual.ATTACH_LEFT, false));
		snaps.put(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/right"), edgeSnap(MAIN_BOUNDS_ID, SlotVisual.ATTACH_RIGHT, false));
		snaps.put(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/player_left"),
				edgeSnap(PLAYER_INVENTORY_BOUNDS_ID, SlotVisual.ATTACH_LEFT, false));
		snaps.put(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/player_right"),
				edgeSnap(PLAYER_INVENTORY_BOUNDS_ID, SlotVisual.ATTACH_RIGHT, false));
		snaps.put(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/player_bottom_left"),
				cornerSnap(PLAYER_INVENTORY_BOUNDS_ID, SlotVisual.ATTACH_BOTTOM_LEFT, true));
		snaps.put(Identifier.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "trashslot/player_bottom"),
				edgeSnap(PLAYER_INVENTORY_BOUNDS_ID, SlotVisual.ATTACH_BOTTOM, true));
		snaps.put(PLAYER_BOTTOM_RIGHT_SNAP_ID, cornerSnap(PLAYER_INVENTORY_BOUNDS_ID, SlotVisual.ATTACH_BOTTOM_RIGHT, false));
		snaps.put(PLAYER_RIGHT_BOTTOM_SNAP_ID, new Snap(Optional.of(new SnapCoordinateProvider.Right(PLAYER_INVENTORY_BOUNDS_ID, -1)),
				Optional.of(new SnapCoordinateProvider.Bottom(PLAYER_INVENTORY_BOUNDS_ID, -24)), SlotVisual.ATTACH_RIGHT_BOTTOM));
		return snaps;
	}

	private static Snap edgeSnap(Identifier rectId, SlotVisual visual, boolean horizontal) {
		if (horizontal) {
			return new Snap(
					Optional.of(new SnapCoordinateProvider.Range(new SnapCoordinateProvider.Left(rectId, getHorizontalRangeStartOffset(visual)),
							new SnapCoordinateProvider.Right(rectId, getHorizontalRangeEndOffset(visual)))),
					Optional.of(visual == SlotVisual.ATTACH_TOP ? new SnapCoordinateProvider.Top(rectId, -15) : new SnapCoordinateProvider.Bottom(rectId, -1)),
					visual);
		}

		return new Snap(
				Optional.of(visual == SlotVisual.ATTACH_LEFT ? new SnapCoordinateProvider.Left(rectId, -15) : new SnapCoordinateProvider.Right(rectId, -1)),
				Optional.of(new SnapCoordinateProvider.Range(new SnapCoordinateProvider.Top(rectId, visual == SlotVisual.ATTACH_LEFT ? 11 : 10),
						new SnapCoordinateProvider.Bottom(rectId, visual == SlotVisual.ATTACH_LEFT ? -24 : -28))),
				visual);
	}

	private static Snap cornerSnap(Identifier rectId, SlotVisual visual, boolean left) {
		return new Snap(Optional.of(left ? new SnapCoordinateProvider.Left(rectId, 8) : new SnapCoordinateProvider.Right(rectId, -24)),
				Optional.of(new SnapCoordinateProvider.Bottom(rectId, -1)), visual);
	}

	private static int getHorizontalRangeStartOffset(SlotVisual visual) {
		return switch (visual) {
			case ATTACH_TOP, ATTACH_BOTTOM -> 12;
			default -> 8;
		};
	}

	private static int getHorizontalRangeEndOffset(SlotVisual visual) {
		return switch (visual) {
			case ATTACH_TOP, ATTACH_BOTTOM -> -28;
			default -> -24;
		};
	}

	private Rect2i getMainBounds(StorageScreenBase<?> storageScreen, AbstractContainerScreenAccessor screenAccessor) {
		int height = screenAccessor.getImageHeight();
		if (storageScreen.getInventoryLabelX() > 8) {
			height -= HEIGHT_OF_PLAYER_INVENTORY_STICKING_OUT;
		}
		return new Rect2i(screenAccessor.getLeftPos(), screenAccessor.getTopPos(), screenAccessor.getImageWidth(), height);
	}

	private Optional<Rect2i> getPlayerInventoryBounds(StorageScreenBase<?> storageScreen, AbstractContainerScreenAccessor screenAccessor) {
		return Optional.of(new Rect2i(getPlayerInventoryLeftSnap(storageScreen, screenAccessor),
				screenAccessor.getTopPos() + screenAccessor.getImageHeight() - HEIGHT_OF_PLAYER_INVENTORY_STICKING_OUT, PLAYER_INVENTORY_WIDTH,
				HEIGHT_OF_PLAYER_INVENTORY_STICKING_OUT));
	}

	private int getPlayerInventoryLeftSnap(StorageScreenBase<?> storageScreen, AbstractContainerScreenAccessor screenAccessor) {
		return screenAccessor.getLeftPos() + storageScreen.getInventoryLabelX() - 7 - 1;
	}
}
