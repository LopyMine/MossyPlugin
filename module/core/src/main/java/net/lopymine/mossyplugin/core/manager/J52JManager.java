package net.lopymine.mossyplugin.core.manager;

import dev.kikugie.fletching_table.extension.FletchingTableExtension;
import lombok.experimental.ExtensionMethod;
import net.lopymine.mossyplugin.core.MossyPluginCore;
import org.gradle.api.Project;

@ExtensionMethod(MossyPluginCore.class)
public class J52JManager {

	public static void apply(Project project) {
		project.getExtensions().configure(FletchingTableExtension.class, (extension) -> {
			var main = extension.getJ52j().register("main");
			main.configure((container) -> container.extension("json", "**/*.json5"));
		});
	}

}
