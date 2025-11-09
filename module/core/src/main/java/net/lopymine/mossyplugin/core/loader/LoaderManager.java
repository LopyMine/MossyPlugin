package net.lopymine.mossyplugin.core.loader;

import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import org.gradle.api.file.FileCopyDetails;
import org.jetbrains.annotations.NotNull;

public interface LoaderManager {

	static LoaderManager of(String loader) {
		if (loader.contains("forge")) {
			return NeoForgeLoaderManager.getInstance();
		} else if (loader.contains("fabric")) {
			return FabricLoaderManager.getInstance();
		} else {
			throw new RuntimeException("Unsupported loader \"%s\"!".formatted(loader));
		}
	}

	void applyPlugins(@NotNull MossyProjectConfigurationData data);

	void applyDependencies(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension extension);

	void configureExtensions(@NotNull MossyProjectConfigurationData data);

	boolean excludeUselessFiles(FileCopyDetails details);

	String getModDependenciesImplementationMethod();

	String getJarTaskName();

	String getAWFileExtension();
}
