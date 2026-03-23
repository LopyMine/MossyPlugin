package net.lopymine.mossyplugin.settings.manager;

import java.nio.file.*;
import java.util.*;

public class GithubManager {

	public static void apply(Path root, List<String> loaderAndVersions) {
		Path path = root.resolve(".github/workflows/build.yml");
		if (!Files.exists(path)) {
			return;
		}
		try {
			String content = new String(Files.readAllBytes(path));

			content = content.replaceAll(
					"platform:\\s*\\[[^\\]]*\\]",
					"platform: [%s]".formatted(String.join(", ", loaderAndVersions))
			);

			Files.write(path, content.getBytes());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
