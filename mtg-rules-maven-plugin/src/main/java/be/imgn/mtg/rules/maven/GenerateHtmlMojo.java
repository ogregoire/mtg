package be.imgn.mtg.rules.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jspecify.annotations.Nullable;

/** Maven plugin goal to generate HTML from MTG Comprehensive Rules text files. */
@Mojo(name = "generate-html", defaultPhase = LifecyclePhase.PRE_SITE)
public class GenerateHtmlMojo extends AbstractMojo {

    /** The input rules text file. */
    @Parameter(property = "mtg-rules.inputFile", required = true)
    @Nullable
    private File inputFile;

    /** The output HTML file. */
    @Parameter(property = "mtg-rules.outputFile", required = true)
    @Nullable
    private File outputFile;

    @Override
    public void execute() throws MojoExecutionException {
        if (inputFile == null) {
            throw new MojoExecutionException("inputFile parameter is required");
        }
        if (outputFile == null) {
            throw new MojoExecutionException("outputFile parameter is required");
        }

        if (!inputFile.exists()) {
            throw new MojoExecutionException("Input file does not exist: " + inputFile);
        }

        // Skip if output is up-to-date
        if (outputFile.exists() && outputFile.lastModified() >= inputFile.lastModified()) {
            getLog().info("Rules HTML is up-to-date, skipping generation");
            return;
        }

        getLog().info("Generating HTML from rules file: " + inputFile);

        try {
            // Read the input file
            var rulesText = Files.readString(inputFile.toPath(), StandardCharsets.UTF_8);

            // Parse the rules
            var parser = new RulesParser();
            var document = parser.parse(rulesText);

            getLog().info("Parsed " + document.chapters().size() + " chapters and "
                    + document.glossary().size() + " glossary terms");

            // Generate HTML
            var generator = new HtmlGenerator();
            var html = generator.generate(document);

            // Write output file
            var outputDir = outputFile.getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                if (!outputDir.mkdirs()) {
                    throw new MojoExecutionException("Failed to create output directory: " + outputDir);
                }
            }

            Files.writeString(outputFile.toPath(), html, StandardCharsets.UTF_8);
            getLog().info("Generated HTML file: " + outputFile);

        } catch (IOException e) {
            throw new MojoExecutionException("Error processing rules file", e);
        }
    }
}
