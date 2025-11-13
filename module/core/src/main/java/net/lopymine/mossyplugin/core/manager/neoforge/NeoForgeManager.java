package net.lopymine.mossyplugin.core.manager.neoforge;

import java.nio.file.Path;
import lombok.experimental.ExtensionMethod;
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

		Parchment parchment = extension.getParchment();
		parchment.getMappingsVersion().set(dependencies.getParchment());
		parchment.getMinecraftVersion().set(dependencies.getMinecraft());

		extension.getValidateAccessTransformers().set(true);
		extension.getAccessTransformers().from("../../src/main/resources/aws/%s-%s.cfg".formatted(data.loaderName(), data.minecraftVersion()));

		extension.runs((container) -> {
			Path runs = project.getRootProject().getProjectDir().toPath().resolve("runs");

			RunModel client = container.create("client");
			client.client();
			client.getGameDirectory().set(runs.resolve("client").toFile());

			RunModel server = container.create("server");
			server.server();
			server.programArgument("--nogui");
			server.getGameDirectory().set(runs.resolve("server").toFile());
		});

		extension.mods((container) -> {
			NamedDomainObjectProvider<ModModel> provider = container.register(project.getProperty("data.mod_id"));
			provider.configure((model) -> {
				JavaPluginExtension javaPlugin = project.getExtensions().getByType(JavaPluginExtension.class);
				model.sourceSet(javaPlugin.getSourceSets().getByName("main"));
			});
		});
	}

}
