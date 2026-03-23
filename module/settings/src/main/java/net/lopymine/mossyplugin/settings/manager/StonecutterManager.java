package net.lopymine.mossyplugin.settings.manager;

import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension;
import java.util.*;
import net.lopymine.mossyplugin.common.MossyUtils;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.NotNull;

public class StonecutterManager {

	public static void apply(@NotNull Settings settings, Map<String, List<String>> projects) {
		StonecutterSettingsExtension stonecutter = settings.getExtensions().getByType(StonecutterSettingsExtension.class);
		stonecutter.create(settings.getRootProject(), (builder) -> {
			String propertyLoader = settings.getProviders().gradleProperty("ci_loader").getOrNull();
			projects.forEach((loader, versions) -> {
				if (propertyLoader != null && !propertyLoader.equals(loader)) {
					return;
				}
				String last = versions.get(versions.size() - 1);
				for (String version : versions) {
					String ver = "%s-%s".formatted(loader, version);
					builder.version(ver, MossyUtils.substringBefore(version, "-"));
					if (version.equals(last)) {
						builder.getVcsVersion().set(ver);
					}
				}
			});
		});
	}

}
