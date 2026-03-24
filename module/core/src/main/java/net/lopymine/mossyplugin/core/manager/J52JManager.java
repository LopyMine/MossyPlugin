package net.lopymine.mossyplugin.core.manager;

import dev.kikugie.fletching_table.extension.FletchingTableExtension;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import net.lopymine.mossyplugin.core.data.MossyProjectConfigurationData;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

@ExtensionMethod(MossyPluginCore.class)
public class J52JManager {

	public static void apply(@NotNull MossyProjectConfigurationData data) {
		Project project = data.project();
		project.getExtensions().configure(FletchingTableExtension.class, (extension) -> {
			var main = extension.getJ52j().register("main");
			main.configure((container) -> container.extension("json", "**/*.json5"));
		});
	}

}
