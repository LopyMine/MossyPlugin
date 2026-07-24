package net.lopymine.mossyplugin.core.util;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of Modrinth projects a test can pull, keyed by the last segment of a "test.&lt;name&gt;.&lt;type&gt;" property.
 * Each type maps to the game folder it belongs in and whether the mod loader is relevant when querying Modrinth
 * (a resource pack or shader is loader agnostic, so only the game version filters it).
 */
@Getter
public enum TestAssetType {

	MODS("mods", "mods", true),
	SHADERS("shaders", "shaderpacks", false),
	RESOURCEPACKS("resourcepacks", "resourcepacks", false);

	private final String key;
	private final String folder;
	private final boolean loaderSpecific;

	TestAssetType(String key, String folder, boolean loaderSpecific) {
		this.key = key;
		this.folder = folder;
		this.loaderSpecific = loaderSpecific;
	}

	public static @Nullable TestAssetType byKey(String key) {
		for (TestAssetType type : values()) {
			if (type.key.equals(key)) {
				return type;
			}
		}
		return null;
	}

}
