package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = UnverifiedJwtCodemod.class,
    testResourceDir = "missing-jwt-signature-check",
    dependencies = {})
public class UnverifiedJwtCodemodTest implements CodemodTestMixin {}
