package net.lopymine.mossyplugin.core.extension;

import java.util.*;
import lombok.Getter;
import org.gradle.api.tasks.Input;
import org.jetbrains.annotations.Nullable;

@Getter
public class MossyCoreProcessResourcesExtension {

	@Input
	@Nullable
	List<String> expandFiles;

	private final Map<String, String> customProperties = new HashMap<>();

	public void customProperty(String id, String value) {
		this.customProperties.put(id, value);
	}

}
