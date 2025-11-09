package net.lopymine.mossyplugin.core.manager;

import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import org.gradle.api.*;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class JavaManager {

	public static void apply(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		MossyPluginCore plugin = data.plugin();

		int javaVersionIndex = plugin.getJavaVersionIndex();
		JavaVersion javaVersion = plugin.getJavaVersion();

		TaskCollection<JavaCompile> collection = project.getTasks().withType(JavaCompile.class);
		for (JavaCompile javaCompile : collection) {
			javaCompile.getOptions().getRelease().set(javaVersionIndex);
		}

		JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
		//javaExtension.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(javaVersion.getMajorVersion()));
		javaExtension.setSourceCompatibility(javaVersion);
		javaExtension.setTargetCompatibility(javaVersion);
	}
}
