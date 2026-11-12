package net.floriankraemer.cognitive_analysis.domain;

/**
 * One analysed method with the file it was found in.
 *
 * @param file the path of the file as it was discovered
 * @param key the method key {@code pkg.Class::method}
 */
public record ScoredMethod(String file, String key, CognitiveMetrics metrics) {}
