package net.lopymine.mossyplugin.core.task;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Setter;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.util.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@Setter
@DisableCachingByDefault
public class DownloadTestAssets extends DefaultTask {

	private String loader;
	private String minecraftVersion;
	private Map<String, String> tests;
	private File testsDirectory;

	@TaskAction
	public void download() {
		deleteRecursively(this.testsDirectory);
		MossyPluginCore.LOGGER.setup(this.getProject());

		if (this.tests.isEmpty()) {
			return;
		}

		for (Entry<String, String> entry : this.tests.entrySet()) {
			String key = entry.getKey();
			String testName = MossyUtils.substringBefore(key, ".");
			String assetKey = MossyUtils.substringSince(key, ".");

			TestAssetType assetType = TestAssetType.byKey(assetKey);
			if (assetType == null) {
				MossyPluginCore.LOGGER.log("Unknown test asset type \"%s\" in \"test.%s\"", assetKey, key);
				continue;
			}

			File directory = this.testsDirectory.toPath().resolve(testName).resolve(assetType.getFolder()).toFile();
			if (!directory.mkdirs()) {
				MossyPluginCore.LOGGER.log("Failed to create test directory \"%s\"", directory);
				continue;
			}

			String queryLoader = assetType.isLoaderSpecific() ? this.loader : null;
			for (String projectId : entry.getValue().trim().split("\\s+")) {
				if (projectId.isBlank()) {
					continue;
				}
				ModrinthFileApi.download(projectId, this.minecraftVersion, queryLoader, directory);
			}
		}
	}

	private static void deleteRecursively(File file) {
		if (!file.exists()) {
			return;
		}
		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children) {
				deleteRecursively(child);
			}
		}
		if (!file.delete()) {
			MossyPluginCore.LOGGER.log("Failed to delete \"%s\"", file);
		}
	}

}
