package net.lopymine.mossyplugin.core.manager.neoforge;

import java.nio.file.Path;
import java.util.*;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.common.MossyUtils;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
import net.neoforged.moddevgradle.dsl.*;
import org.gradle.api.*;
import org.gradle.api.plugins.JavaPluginExtension;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class NeoForgeManager {

	public static void apply(@NotNull MossyProjectConfigurationData data, ModDevExtension extension, MossyCoreDependenciesExtension dependencies) {
		Project project = data.project();

		Properties personalProperties = project.getPersonalProperties();

		String playerNickname = MossyUtils.getPlayerNickname(personalProperties);
		UUID playerUuid = MossyUtils.getPlayerUuid(personalProperties);
		Object quickPlayWorld = personalProperties.get("quick_play_world");
		//Object pathToSpongeMixin = personalProperties.get("absolute_path_to_sponge_mixin");

		Parchment parchment = extension.getParchment();
		parchment.getMappingsVersion().set(dependencies.getParchment());
		parchment.getMinecraftVersion().set(dependencies.getMinecraft());

		extension.getValidateAccessTransformers().set(true);
		extension.getAccessTransformers().from("../../src/main/resources/aws/%s-%s.cfg".formatted(data.loaderName(), data.minecraftVersion()));

		String sides = data.project().getProperty("data.sides").toLowerCase(Locale.ROOT);
		boolean createClient = sides.equals("client") || sides.equals("both");
		boolean createServer = sides.equals("server") || sides.equals("both");

		extension.runs((container) -> {
			Path runs = project.getRootProject().getProjectDir().toPath().resolve("runs");

			if (createClient) {
				RunModel client = container.create("client");
				client.client();
				client.getGameDirectory().set(runs.resolve("client").toFile());
				addProgramArgument(client, "--username ", playerNickname);
				addProgramArgument(client, "--uuid ", playerUuid);
				addProgramArgument(client, "--quickPlaySingleplayer ", quickPlayWorld);
				//addVMArgument(client, "-javaagent:", pathToSpongeMixin); doesn't required
			}

			if (createServer) {
				RunModel server = container.create("server");
				server.server();
				server.programArgument("--nogui");
				server.getGameDirectory().set(runs.resolve("server").toFile());
			}
		});

		extension.mods((container) -> {
			NamedDomainObjectProvider<ModModel> provider = container.register(project.getProperty("data.mod_id"));
			provider.configure((model) -> {
				JavaPluginExtension javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
				model.sourceSet(javaPlugin.getSourceSets().getByName("main"));
			});
		});
	}

	private static void addProgramArgument(RunModel client, String key, Object argument) {
		if (argument == null || argument.toString().equals("none")) {
			return;
		}
		client.programArgument(key);
		client.programArgument(argument.toString());
	}

	private static void addVMArgument(RunModel client, String key, Object argument) {
		if (argument == null || argument.toString().equals("none")) {
			return;
		}
		client.jvmArgument(key);
		client.jvmArgument(argument.toString());
	}

}
