package net.lopymine.mossyplugin.core.manager.forge;

//import java.nio.file.Path;
//import lombok.experimental.ExtensionMethod;
//import net.lopymine.mossyplugin.core.MossyPluginCore;
//import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
//import net.lopymine.mossyplugin.core.extension.MossyCoreDependenciesExtension;
//import net.minecraftforge.gradle.common.util.*;
//import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension;
//import org.gradle.api.*;
//import org.jetbrains.annotations.NotNull;
//
//@ExtensionMethod(MossyPluginCore.class)
//public class ForgeManager {
//
//	public static void apply(@NotNull MossyProjectConfigurationData data, MossyCoreDependenciesExtension extension) {
//		Project project = data.project();
//		LegacyForgeExtension forge = project.getExtensions().getByType(LegacyForgeExtension.class);
//
//		forge.mappings("parchment", extension.getParchment());
//
//		forge.getAccessTransformers().from("../../src/main/resources/aws/neoforge-%s.cfg".formatted(data.minecraftVersion()));
//
//		Path workingDirectory = project.getRootProject().getProjectDir().toPath().resolve("runs");
//		NamedDomainObjectContainer<RunConfig> runs = forge.getRuns();
//		RunConfig client = runs.create("client");
//		client.client(true);
//		client.setWorkingDirectory(workingDirectory.resolve("client").toAbsolutePath().toString());
//
//		RunConfig server = runs.create("server");
//		server.client(false);
//		server.arg("--nogui");
//		server.setWorkingDirectory(workingDirectory.resolve("server").toAbsolutePath().toString());
//
//		runs.configureEach((run) -> {
//			run.arg("-mixin.config=%s.mixins.json".formatted(data.project().getProperty("data.mod_id")));
//		});
//	}
//
//}
