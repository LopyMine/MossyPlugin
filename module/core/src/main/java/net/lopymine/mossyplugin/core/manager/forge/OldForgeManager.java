package net.lopymine.mossyplugin.core.manager.forge;

import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.minecraftforge.gradle.common.util.*;
import org.gradle.api.*;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class OldForgeManager {

	public static void apply(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension extension) {
		Project project = data.project();

		Properties personalProperties = project.getPersonalProperties();

		String playerNickname = MossyUtils.getPlayerNickname(personalProperties);
		Map<String, UUID> altAccounts = MossyUtils.getAltAccounts(personalProperties);
		UUID playerUuid = MossyUtils.getPlayerUuid(personalProperties);
		Object quickPlayWorld = personalProperties.get("quick_play_world");

		MinecraftExtension forge = project.getExtensions().getByType(MinecraftExtension.class);

		forge.mappings("parchment", extension.getParchment());
		forge.getCopyIdeResources().set(true);
		forge.getAccessTransformers().from("../../src/main/resources/aws/forge-%s.cfg".formatted(data.minecraftVersion()));

		String sides = data.project().getProperty("data.sides").toLowerCase(Locale.ROOT);
		boolean createClient = sides.equals("client") || sides.equals("both");
		boolean createServer = sides.equals("server") || sides.equals("both");

		NamedDomainObjectContainer<RunConfig> container = forge.getRuns();

		Path runs = project.getRootProject().getProjectDir().toPath().resolve("runs");

		if (createClient) {
			RunConfig client = container.create("client");
			client.client(true);
			client.setWorkingDirectory(runs.resolve("client").toAbsolutePath().toString());

			addProgramArg(client, "--username", playerNickname);
			addProgramArg(client, "--uuid", playerUuid);
			addProgramArg(client, "--quickPlaySingleplayer", quickPlayWorld);

			for (Entry<String, UUID> entry : altAccounts.entrySet()) {
				String runName = "client_" + entry.getKey();

				RunConfig altClient = container.create(runName);
				altClient.client(true);
				altClient.setWorkingDirectory(runs.resolve(runName).toAbsolutePath().toString());

				addProgramArg(altClient, "--username", entry.getKey());
				addProgramArg(altClient, "--uuid", entry.getValue());
				addProgramArg(altClient, "--quickPlaySingleplayer", quickPlayWorld);

				//altClient.getIdeName().set("%s / %s / client / %s".formatted(platform, data.minecraftVersion(), entry.getKey()));
			}

			//client.getIdeName().set("%s / %s / %s".formatted(platform, data.minecraftVersion(), "client"));
		}

		if (createServer) {
			RunConfig server = container.create("server");
			server.client(false);
			server.arg("--nogui");
			server.setWorkingDirectory(runs.resolve("server").toAbsolutePath().toString());
			//server.getIdeName().set("%s / %s / %s".formatted(platform, data.minecraftVersion(), "server"));
		}
	}

	private static void addProgramArg(RunConfig client, String key, Object argument) {
		if (argument == null || argument.toString().equals("none")) {
			return;
		}
		client.args(key, argument.toString());
	}

}
