package io.pixee.codefixer.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pixee.codetf.CodeTFReport;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

/** This is the connective tissue between the process startup and the weaving process. */
public final class JavaFixitCliRun {

  private final SourceDirectoryLister directorySearcher;
  private final SourceWeaver javaSourceWeaver;
  private final FileWeaver fileWeaver;
  private final VisitorAssembler visitorAssembler;
  private final CodeTFReportGenerator reportGenerator;

  public JavaFixitCliRun() {
    this.directorySearcher = SourceDirectoryLister.createDefault();
    this.javaSourceWeaver = SourceWeaver.createDefault();
    this.fileWeaver = FileWeaver.createDefault();
    this.visitorAssembler = VisitorAssembler.createDefault();
    this.reportGenerator = CodeTFReportGenerator.createDefault();
  }

  /**
   * Performs the main logic of the application.
   *
   * <p>Note that includes and excludes can both be specified, and "includes win" unless the exclude
   * has a longer matching path.
   *
   * @param defaultRuleSetting the default setting for every rule
   * @param ruleExceptions the rules that should be considered to have the opposite default rule
   *     setting
   * @param sarifs the paths to SARIF files that tool
   * @param repositoryRoot the path to the repository root
   * @param includePatterns the patterns to include, null and empty list means all files are scanned
   * @param excludePatterns the patterns to exclude, null and empty list means all files are scanned
   * @param output the output file path
   * @return the CCF report describing the scan/patch process we undertook
   */
  public CodeTFReport run(
      final DefaultRuleSetting defaultRuleSetting,
      final List<String> ruleExceptions,
      final List<File> sarifs,
      final File repositoryRoot,
      final List<String> includePatterns,
      final List<String> excludePatterns,
      final File output,
      final boolean verbose)
      throws IOException {

    configureLogging(verbose);

    LOG.debug("Default rule setting: {}", defaultRuleSetting.getDescription());
    LOG.debug("Exceptions to the default rule setting: {}", ruleExceptions);
    LOG.debug("Repository path: {}", repositoryRoot);
    LOG.debug("Output: {}", output);
    LOG.debug("Includes: {}", includePatterns.size());
    LOG.debug("Excludes: {}", excludePatterns.size());

    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    // find the java source directories -- avoid test directories or other incidental java code
    LOG.debug("Scanning for Java source directories");

    // parse the includes & exclude rules we'll need for all the scanning
    final IncludesExcludes includesExcludes =
        IncludesExcludes.fromConfiguration(repositoryRoot, includePatterns, excludePatterns);

    final List<SourceDirectory> sourceDirectories =
        directorySearcher.listJavaSourceDirectories(List.of(repositoryRoot));

    LOG.debug("Scanning {} Java source directories", sourceDirectories.size());

    // get the Java code visitors
    RuleContext ruleContext = RuleContext.of(defaultRuleSetting, ruleExceptions);
    final List<VisitorFactory> factories =
        visitorAssembler.assembleJavaCodeScanningVisitorFactories(
            repositoryRoot, ruleContext, sarifs);

    List<String> allJavaFiles = new ArrayList<>();
    sourceDirectories.forEach(
        sourceDirectory ->
            allJavaFiles.addAll(
                sourceDirectory.files().stream()
                    .filter(file -> includesExcludes.shouldInspect(new File(file)))
                    .collect(Collectors.toList())));

    LOG.debug("Scanning following files: {}", String.join(",", allJavaFiles));

    // run the Java code visitors
    final var javaSourceWeaveResult =
        javaSourceWeaver.weave(sourceDirectories, allJavaFiles, factories, includesExcludes);

    // get the non-Java code visitors
    final List<FileBasedVisitor> fileBasedVisitors =
        visitorAssembler.assembleFileVisitors(ruleContext);

    // run the non-Java code visitors
    final var fileBasedWeaveResults =
        fileWeaver.weave(
            fileBasedVisitors, repositoryRoot, javaSourceWeaveResult, includesExcludes);

    // merge the results into one
    final var allWeaveResults = merge(javaSourceWeaveResult, fileBasedWeaveResults);
    LOG.info("Analysis complete!");
    LOG.info("\t{} changes", allWeaveResults.changedFiles().size());
    LOG.info("\t{} errors", allWeaveResults.unscannableFiles().size());

    // clean up
    stopWatch.stop();
    final long elapsed = stopWatch.getTime();

    CodeTFReport report =
        reportGenerator.createReport(
            repositoryRoot, includePatterns, excludePatterns, allWeaveResults, elapsed);

    // write out the JSON
    ObjectMapper mapper = new ObjectMapper();
    FileUtils.write(output, mapper.writeValueAsString(report), StandardCharsets.UTF_8);
    return report;
  }

  private void configureLogging(final boolean verbose) {
    LoggerContext ctx = (LoggerContext) LogManager.getContext(getClass().getClassLoader(), false);
    org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
    LoggerConfig rootLoggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    rootLoggerConfig.setLevel(Level.ERROR);

    String loggerName = getClass().getPackageName();
    LoggerConfig pixeeConfig =
        new LoggerConfig(loggerName, verbose ? Level.DEBUG : Level.INFO, false);
    pixeeConfig.setParent(rootLoggerConfig);
    ConsoleAppender appender =
        ConsoleAppender.createDefaultAppenderForLayout(PatternLayout.createDefaultLayout());
    pixeeConfig.addAppender(appender, Level.ALL, null);
    config.addLogger(loggerName, pixeeConfig);
    ctx.updateLoggers();
  }

  /**
   * When we need to combine the results of multiple analyses, we can combine them with a method
   * like this one. There is notably some loss of fidelity here when the two sets are combined, but
   * hopefully all the changers have different domains over different types of files, so there
   * should be little chance of collision.
   */
  private WeavingResult merge(final WeavingResult result1, final WeavingResult result2) {
    var combinedChangedFiles = new HashSet<ChangedFile>();
    combinedChangedFiles.addAll(result1.changedFiles());
    combinedChangedFiles.addAll(result2.changedFiles());

    var combinedUnscannableFiles = new HashSet<String>();
    combinedUnscannableFiles.addAll(result1.unscannableFiles());
    combinedUnscannableFiles.addAll(result2.unscannableFiles());

    return WeavingResult.createDefault(combinedChangedFiles, combinedUnscannableFiles);
  }

  private static Logger LOG = LogManager.getLogger(JavaFixitCliRun.class);
}
