package net.lopymine.mossyplugin.core.manager;

import dev.kikugie.stonecutter.build.StonecutterBuildExtension;
import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class StonecutterManager {

	public static void apply(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		MossyPluginCore plugin = data.plugin();

		StonecutterBuildExtension stonecutter = project.getStonecutter();

		String mcVersion = plugin.getProjectMultiVersion().projectVersion();
		Map<String, String> properties = project.getMossyProperties("data");
		properties.putAll(project.getMossyProperties("build"));
		Map<String, String> dependencies = project.getMossyProperties("dep");
		properties.putAll(dependencies);
		properties.put("java", String.valueOf(plugin.getJavaVersionIndex()));
		properties.put("minecraft", mcVersion);
		properties.put("fabric_api_id", project.getStonecutter().compare("1.19.1", mcVersion) >= 0 ? "fabric" : "fabric-api");
		properties.put("mod_version", project.getVersion().toString());

		properties.forEach((key, value) -> {
			stonecutter.getSwaps().put(key, getFormatted(value));
		});

		dependencies.forEach((modId, version) -> {
			stonecutter.getConstants().put(modId, !version.equals("unknown"));
		});

		Arrays.stream(project.getProperty("mod_loaders").split(" ")).forEach((loader) -> {
			stonecutter.getConstants().put(loader, project.getName().startsWith(loader));
		});
	}

	private static @NotNull String getFormatted(String modVersion) {
		return "\"%s\";".formatted(modVersion);
	}
}
