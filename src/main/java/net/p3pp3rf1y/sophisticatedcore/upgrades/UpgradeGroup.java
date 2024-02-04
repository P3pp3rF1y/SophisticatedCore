package net.p3pp3rf1y.sophisticatedcore.upgrades;

public final class UpgradeGroup {

	public static final UpgradeGroup NONE = new UpgradeGroup("none", "none", true);
	private final String name;
	private final String translName;
	private final boolean isSolo;

	public UpgradeGroup(String name, String translName) {
		this(name, translName, false);
	}

	private UpgradeGroup(String name, String translName, boolean isSolo) {
		this.name = name;
		this.translName = translName;
		this.isSolo = isSolo;
	}

	public String name() {
		return name;
	}

	public String translName() {
		return translName;
	}

	public boolean isSolo() {
		return isSolo;
	}
}
