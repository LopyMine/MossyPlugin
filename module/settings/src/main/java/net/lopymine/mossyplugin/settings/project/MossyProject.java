package net.lopymine.mossyplugin.settings.project;

import net.lopymine.mossyplugin.settings.loader.LoaderManager;

public record MossyProject(
		String projectName,
		String loaderName,
		String minecraftVersion,
		LoaderManager loaderManager
) {

}
