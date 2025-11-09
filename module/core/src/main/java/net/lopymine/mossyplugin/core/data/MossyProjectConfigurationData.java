package net.lopymine.mossyplugin.core.data;

import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.loader.LoaderManager;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public record MossyProjectConfigurationData(
		MossyPluginCore plugin,
		String projectName,
		String loaderName,
		String minecraftVersion,
		LoaderManager loaderManager,
		Project project
) {

	public static MossyProjectConfigurationData create(@NotNull Project project, MossyPluginCore plugin) {
		String projectName = project.getName();
		String loaderName = MossyUtils.substringBefore(projectName, "-");
		String minecraftVersion = MossyUtils.substringSince(projectName, "-");
		LoaderManager loaderManager = LoaderManager.of(loaderName);
		return new MossyProjectConfigurationData(plugin, projectName, loaderName, minecraftVersion, loaderManager, project);
	}
}
