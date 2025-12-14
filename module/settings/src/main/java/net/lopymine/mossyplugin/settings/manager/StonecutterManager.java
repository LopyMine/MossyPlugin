package net.lopymine.mossyplugin.settings.manager;

import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension;
import java.util.*;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.NotNull;

public class StonecutterManager {

	public static void apply(@NotNull Settings settings, Map<String, List<String>> projects) {
		StonecutterSettingsExtension stonecutter = settings.getExtensions().getByType(StonecutterSettingsExtension.class);
		stonecutter.create(settings.getRootProject(), (builder) -> {
			projects.forEach((loader, versions) -> {
				String last = versions.get(versions.size() - 1);
				for (String version : versions) {
					String ver = "%s-%s".formatted(loader, version);
					builder.version(ver, version);
					if (version.equals(last)) {
						builder.getVcsVersion().set(ver);
					}
				}
			});
		});
	}

}
