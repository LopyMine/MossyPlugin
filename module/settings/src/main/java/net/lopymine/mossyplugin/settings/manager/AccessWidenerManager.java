package net.lopymine.mossyplugin.settings.manager;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import net.lopymine.mossyplugin.settings.MossyPluginSettings;
import net.lopymine.mossyplugin.settings.project.MossyProject;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.*;

public class AccessWidenerManager {

	public static void apply(@NotNull Settings settings, List<MossyProject> projects) {
		Path path = settings.getRootDir().toPath();
		for (MossyProject project : projects) {
			createExampleAccessWidener(path, project);
		}
	}

	public static void createExampleAccessWidener(Path rootProject, MossyProject project) {
		File awFile = createAWFile(rootProject, project);
		if (awFile == null) {
			return;
		}

		try (FileWriter writer = new FileWriter(awFile)) {
			project.loaderManager().fillAWWillExampleText(writer, project.minecraftVersion());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		MossyPluginSettings.LOGGER.log("Successfully created AW for " + project);
	}

	@Nullable
	private static File createAWFile(Path rootProject, MossyProject project) {
		String projectName = project.projectName();

		Path awsFolder = rootProject.resolve("src/main/resources/aws/");
		File awsFolderFile = awsFolder.toFile();
		if (!awsFolderFile.exists() && !awsFolderFile.mkdirs()) {
			MossyPluginSettings.LOGGER.log("Failed to get or create AW folder for " + projectName);
			return null;
		}

		File versionedAWFile = awsFolder.resolve("%s.%s".formatted(projectName, project.loaderManager().getAWExtension())).toFile();
		if (versionedAWFile.exists()) {
			return null;
		}

		try {
			if (!versionedAWFile.createNewFile()) {
				MossyPluginSettings.LOGGER.log("Failed to create AW file for " + projectName);
				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return versionedAWFile;
	}

}
