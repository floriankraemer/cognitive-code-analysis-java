/**
 * The public, stable API for custom reporters.
 *
 * <p>A reporter implements {@link net.floriankraemer.cognitive_analysis.report.spi.CognitiveReporter} or
 * {@link net.floriankraemer.cognitive_analysis.report.spi.ChurnReporter}, has a public constructor without
 * parameters, and is registered under a key in the {@code customReporters} section of the configuration.
 * Everything in this package is immutable and independent of the internals of the analysis.
 */
package net.floriankraemer.cognitive_analysis.report.spi;
