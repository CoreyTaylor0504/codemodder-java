package io.pixee.codefixer.java.protections;

import com.google.common.annotations.VisibleForTesting;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * This type corrects simple and obvious verb tampering vulnerabilities. It simply nukes
 * &lt;http-method&gt; entries in web.xml files.
 */
public final class VerbTamperingVisitor extends RegexTextVisitor {

  public VerbTamperingVisitor() {
    super(
        file -> "web.xml".equalsIgnoreCase(file.getName()), httpMethod, verbTamperingRuleId, true);
  }

  @Override
  public String ruleId() {
    return verbTamperingRuleId;
  }

  @Override
  public @NotNull String getReplacementFor(final String matchingSnippet) {
    return "";
  }

  private static final Pattern httpMethod =
      Pattern.compile(
          "<http-method(\\s*)>[a-zA-Z\\s*]+</http-method>", Pattern.MULTILINE | Pattern.DOTALL);
  @VisibleForTesting static final String verbTamperingRuleId = "pixee:java/verb-tampering-jakarta";
}
