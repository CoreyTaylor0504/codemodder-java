package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.ObjectCreationPredicateFactory;
import io.pixee.codefixer.java.ObjectCreationTransformingModifierVisitor;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import java.io.File;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This visitor replaces instance creation of {@link java.util.Random} with {@link
 * java.security.SecureRandom}.
 */
public final class WeakPRNGVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    List<Predicate<ObjectCreationExpr>> predicates =
        List.of(
            ObjectCreationPredicateFactory.withArgumentCount(0),
            ObjectCreationPredicateFactory.withType("Random")
                .or(ObjectCreationPredicateFactory.withType("java.util.Random")));

    Transformer<ObjectCreationExpr, ObjectCreationExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<ObjectCreationExpr> transform(
              final ObjectCreationExpr objectCreationExpr, final FileWeavingContext context) {
            objectCreationExpr.setType(new ClassOrInterfaceType(SecureRandom.class.getName()));
            Weave weave =
                Weave.from(objectCreationExpr.getRange().get().begin.line, secureRandomRuleId);
            return new TransformationResult<>(Optional.empty(), weave);
          }
        };

    return new ObjectCreationTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return secureRandomRuleId;
  }

  private static final String secureRandomRuleId = "pixee:java/secure-random";
}
