plugins {
    id("io.openpixee.codetl.java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

spotless {
    java {
        // TODO https://www.notion.so/pixee/CodeTL-Cleans-Up-Java-Imports-After-Transformation-3db498f1e23d498b89c4e9bb1495d624
        targetExclude("src/test/java/com/acme/testcode/*.java")
    }
}

dependencies {
    annotationProcessor(libs.autovalue.annotations)
    annotationProcessor(libs.picocli.codegen)

    compileOnly(libs.jetbrains.annotations)

    implementation(libs.codescan.sarif)
    implementation("io.github.pixee:codetf-java:0.0.2") // TODO inline
    implementation(libs.commons.collections4)
    implementation(libs.commons.lang3)
    implementation(libs.contrast.sarif)
    implementation(libs.immutables)
    implementation(libs.gson)
    implementation(libs.jackson.core)
    implementation(libs.jackson.yaml)
    implementation(libs.javadiff)
    implementation(libs.javaparser.core)
    implementation(libs.javaparser.symbolsolver.core)
    implementation(libs.javaparser.symbolsolver.logic)
    implementation(libs.javaparser.symbolsolver.model)
    implementation(libs.jfiglet)
    implementation(libs.juniversalchardet)
    implementation(libs.logback.classic)
    implementation(libs.maven.model)
    implementation("io.openpixee:java-jdbc-parameterizer:0.0.7") // TODO inline
    implementation(libs.openpixee.toolkit)
    implementation(libs.openpixee.toolkit.xstream)
    implementation(libs.picocli)
    implementation(libs.progressbar)
    implementation(libs.slf4j.api)

    testCompileOnly(libs.jetbrains.annotations)

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.jgit)
    testImplementation(testlibs.mockito)

    testRuntimeOnly(testlibs.junit.jupiter.engine)

    // TODO move test fixtures to a different source set
    testImplementation(testcodelibs.commons.fileupload)
    testImplementation(testcodelibs.jwt)
    testImplementation(testcodelibs.owasp)
    testImplementation(testcodelibs.servlet)
    testImplementation(testcodelibs.spring.web)
    testImplementation(testcodelibs.xstream)
}

tasks.test {
    useJUnitPlatform()
}