package net.lopymine.mossyplugin.core.manager.fabric;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import lombok.experimental.ExtensionMethod;
import net.fabricmc.loom.api.LoomGradleExtensionAPI;
import net.fabricmc.loom.configuration.ide.RunConfigSettings;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.util.TestRuns;
import org.gradle.api.*;
import org.jetbrains.annotations.*;

@ExtensionMethod(MossyPluginCore.class)
public class LoomManager {

	@SuppressWarnings("UnstableApiUsage")
	public static void apply(@NotNull MossyProjectConfigurationData data, LoomGradleExtensionAPI loom) {
		Project project = data.project();

		String modId = project.getProperty("data.mod_id");
		File file = project.getRootFile("src/main/resources/aws/%s.%s".formatted(project.getName(), data.loaderManager().getAWFileExtension(data)));

		// Mixins and AWs

		loom.getMixin().getDefaultRefmapName().set("%s.refmap.json".formatted(modId));
		loom.getAccessWidenerPath().set(file);

		// Run Configs

		Properties personalProperties = project.getPersonalProperties();

		String playerNickname = MossyUtils.getPlayerNickname(personalProperties);
		UUID playerUuid = MossyUtils.getPlayerUuid(personalProperties);
		Map<String, UUID> altAccounts = MossyUtils.getAltAccounts(personalProperties);
		Object quickPlayWorld = personalProperties.get("quick_play_world");
		Object pathToSpongeMixin = personalProperties.get("absolute_path_to_sponge_mixin");

		String sides = data.project().getProperty("data.sides").toLowerCase(Locale.ROOT);
		boolean createClient = sides.equals("client") || sides.equals("both");
		boolean createServer = sides.equals("server") || sides.equals("both");

		NamedDomainObjectContainer<RunConfigSettings> runConfigs = loom.getRunConfigs();
		for (RunConfigSettings runConfig : runConfigs) {
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

			runConfig.getAppendProjectPathToConfigName().set(false);
			runConfig.setName("fabric / %s / %s".formatted(data.minecraftVersion(), runConfig.getEnvironment()));
			runConfig.getAppendProjectPathToConfigName().set(false);
		}

		RunConfigSettings client = runConfigs.getByName("client");

		for (Entry<String, UUID> entry : altAccounts.entrySet()) {
			RunConfigSettings altClient = runConfigs.create("fabric-%s-client-%s".formatted(data.minecraftVersion(), entry.getKey()));
			altClient.inherit(client);

			altClient.setRunDir("../../runs/" + "client_" + entry.getKey());
			addProgramArg(altClient, "--username", entry.getKey());
			addProgramArg(altClient, "--uuid", entry.getValue());
			addProgramArg(altClient, "--quickPlaySingleplayer", quickPlayWorld);

			altClient.getAppendProjectPathToConfigName().set(false);
			altClient.setName("fabric / %s / client / %s".formatted(data.minecraftVersion(), entry.getKey()));
			altClient.getAppendProjectPathToConfigName().set(false);
		}

		if (!TestRuns.isEnabled(project)) {
			return;
		}

		if (createClient) {
			RunConfigSettings clientTest = runConfigs.create("clientTest");
			clientTest.inherit(client);
			clientTest.setRunDir(TestRuns.getRelativeRunDirectory(project, "client"));
			configureTestRun(clientTest, "fabric / %s / test / client".formatted(data.minecraftVersion()));

			for (Entry<String, UUID> entry : altAccounts.entrySet()) {
				RunConfigSettings altClientTest = runConfigs.create("clientTest_%s".formatted(entry.getKey()));
				altClientTest.inherit(client);
				altClientTest.setRunDir(TestRuns.getRelativeRunDirectory(project, "client_%s".formatted(entry.getKey())));

				addProgramArg(altClientTest, "--username", entry.getKey());
				addProgramArg(altClientTest, "--uuid", entry.getValue());
				addProgramArg(altClientTest, "--quickPlaySingleplayer", quickPlayWorld);

				configureTestRun(altClientTest, "fabric / %s / test / client / %s".formatted(data.minecraftVersion(), entry.getKey()));
			}
		}

		if (createServer) {
			RunConfigSettings serverTest = runConfigs.create("serverTest");
			serverTest.inherit(runConfigs.getByName("server"));
			serverTest.setRunDir(TestRuns.getRelativeRunDirectory(project, "server"));
			configureTestRun(serverTest, "fabric / %s / test / server".formatted(data.minecraftVersion()));
		}
	}

	private static void configureTestRun(@NotNull RunConfigSettings settings, String name) {
		settings.getAppendProjectPathToConfigName().set(false);
		settings.setName(name);
		settings.setIdeConfigGenerated(false);
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
