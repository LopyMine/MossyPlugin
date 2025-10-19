package net.lopymine.mossyplugin.stonecutter.tasks;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import lombok.*;
import org.gradle.api.*;
import org.gradle.api.tasks.*;

@Setter
@Getter
public class GeneratePublishWorkflowsForEachVersionTask extends DefaultTask {

	@Input
	List<String> multiVersions;

	@TaskAction
	public void generate() {
		Project project = this.getProject();
		File file = project.file(".github/workflows/");
		if (!file.exists() && !file.mkdirs()) {
			return;
		}
		for (String multiVersion : this.getMultiVersions()) {
			try {
				File workflowFile = file.toPath().resolve("publish_%s.yml".formatted(multiVersion)).toFile();
				if (workflowFile.exists()) {
					continue;
				}
				if (!workflowFile.createNewFile()) {
					continue;
				}
				String strip = """
						# Generated workflow by task
						
						name: Publish MULTI_VERSION_ID Version
						on: [workflow_dispatch] # Manual trigger
						
						permissions:
						  contents: write
						
						jobs:
						  build:
						    runs-on: ubuntu-22.04
						    container:
						      image: mcr.microsoft.com/openjdk/jdk:21-ubuntu
						      options: --user root
						    steps:
						      - uses: actions/checkout@v4
						      - name: make gradle wrapper executable
						        run: chmod +x ./gradlew
						      - name: Publish MULTI_VERSION_ID Mod Version
						        run: ./gradlew buildAndCollect+MULTI_VERSION_ID publish+MULTI_VERSION_ID
						        env:
						          CURSEFORGE_API_KEY: ${{ secrets.CURSEFORGE_API_KEY }}
						          MODRINTH_API_KEY: ${{ secrets.MODRINTH_API_KEY }}
						""".replaceAll("MULTI_VERSION_ID", multiVersion).stripIndent().strip();
				Files.write(workflowFile.toPath(), strip.getBytes());
			} catch (Exception ignored) {
			}

		}
	}

}
