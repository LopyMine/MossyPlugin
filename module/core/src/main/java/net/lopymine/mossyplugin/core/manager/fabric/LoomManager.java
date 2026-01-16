package net.lopymine.mossyplugin.core.manager.fabric;

import java.io.File;
import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import org.gradle.api.Project;
import org.jetbrains.annotations.*;

@ExtensionMethod(MossyPluginCore.class)
public class LoomManager {

	@SuppressWarnings("UnstableApiUsage")
	public static void apply(@NotNull MossyProjectConfigurationData data, LoomGradleExtensionAPI loom) {
		Project project = data.project();

		String modId = project.getProperty("data.mod_id");
		File file = project.getRootFile("src/main/resources/aws/%s.accesswidener".formatted(project.getName()));

		// Mixins and AWs

		loom.getMixin().getDefaultRefmapName().set("%s.refmap.json".formatted(modId));
		loom.getAccessWidenerPath().set(file);

		// Run Configs

		Properties personalProperties = project.getPersonalProperties();

		String playerNickname = MossyUtils.getPlayerNickname(personalProperties);
		UUID playerUuid = MossyUtils.getPlayerUuid(personalProperties);
		Object quickPlayWorld = personalProperties.get("quick_play_world");
		Object pathToSpongeMixin = personalProperties.get("absolute_path_to_sponge_mixin");

		String sides = data.project().getProperty("data.sides").toLowerCase(Locale.ROOT);
		boolean createClient = sides.equals("client") || sides.equals("both");
		boolean createServer = sides.equals("server") || sides.equals("both");

		for (RunConfigSettings runConfig : loom.getRunConfigs()) {
			boolean disableServer = runConfig.getEnvironment().equals("server") && !createServer;
			boolean disableClient = runConfig.getEnvironment().equals("client") && !createClient;
			runConfig.setIdeConfigGenerated(!disableServer && !disableClient);

			runConfig.setRunDir("../../runs/" + runConfig.getEnvironment());

			if (runConfig.getEnvironment().equals("client") && createClient) {
				addProgramArg(runConfig, "--username", playerNickname);
				addProgramArg(runConfig, "--uuid", playerUuid);
				addProgramArg(runConfig, "--quickPlaySingleplayer", quickPlayWorld);
				addVMArg(runConfig, "-javaagent", pathToSpongeMixin);
			}
 		}
	}

	@SuppressWarnings("all")
	private static void addVMArg(RunConfigSettings settings, String propertyKey, @Nullable Object propertyValue) {
		if (propertyValue == null || propertyValue.toString().equals("none")) {
			return;
		}
		settings.getVmArgs().add("%s:%s".formatted(propertyKey, propertyValue.toString()));
	}

	private static void addProgramArg(RunConfigSettings settings, String propertyKey, @Nullable Object propertyValue) {
		if (propertyValue == null || propertyValue.toString().equals("none")) {
			return;
		}
		List<String> programArgs = settings.getProgramArgs();
		programArgs.add(propertyKey);
		programArgs.add(propertyValue.toString());
	}
}
