package net.floriankraemer.cognitive_analysis.domain;

import net.floriankraemer.cognitive_analysis.config.MetricName;


public class CognitiveMetrics {

  private String file;
  private String className;
  private String methodName;
  private String packageName;

  private int ifCount;
  private double ifCountWeight;
  private Delta ifCountDelta;

  private int elseCount;
  private double elseCountWeight;
  private Delta elseCountDelta;

  private int loopCount;
  private double loopCountWeight;
  private Delta loopCountDelta;

  private int switchCount;
  private double switchCountWeight;
  private Delta switchCountDelta;

  private int methodCallCount;
  private double methodCallCountWeight;
  private Delta methodCallCountDelta;

  private int returnCount;
  private double returnCountWeight;
  private Delta returnCountDelta;

  private int argCount;
  private double argCountWeight;
  private Delta argCountDelta;

  private int lineNumber;
  private double lineNumberWeight;
  private Delta lineNumberDelta;

  private int ifNestingLevel;
  private double ifNestingLevelWeight;
  private Delta ifNestingLevelDelta;

  private int tryCatchNestingLevel;
  private double tryCatchNestingLevelWeight;
  private Delta tryCatchNestingLevelDelta;

  private int lineCount;
  private double lineCountWeight;
  private Delta lineCountDelta;

  private int variableCount;
  private double variableCountWeight;
  private Delta variableCountDelta;

  private int propertyCallCount;
  private double propertyCallCountWeight;
  private Delta propertyCallCountDelta;

  private double cognitiveComplexity;

  public CognitiveMetrics() {
  }

  public CognitiveMetrics(String className, String methodName) {
    this.className = className;
    this.methodName = methodName;
  }

  /** The raw count of a scored metric. */
  public int value(MetricName name) {
    return switch (name) {
      case LINE_COUNT -> lineCount;
      case ARG_COUNT -> argCount;
      case RETURN_COUNT -> returnCount;
      case VARIABLE_COUNT -> variableCount;
      case PROPERTY_CALL_COUNT -> propertyCallCount;
      case IF_COUNT -> ifCount;
      case IF_NESTING_LEVEL -> ifNestingLevel;
      case ELSE_COUNT -> elseCount;
    };
  }

  public void setWeight(MetricName name, double weight) {
    switch (name) {
      case LINE_COUNT -> lineCountWeight = weight;
      case ARG_COUNT -> argCountWeight = weight;
      case RETURN_COUNT -> returnCountWeight = weight;
      case VARIABLE_COUNT -> variableCountWeight = weight;
      case PROPERTY_CALL_COUNT -> propertyCallCountWeight = weight;
      case IF_COUNT -> ifCountWeight = weight;
      case IF_NESTING_LEVEL -> ifNestingLevelWeight = weight;
      case ELSE_COUNT -> elseCountWeight = weight;
    }
  }

  // Increment methods
  public void incrementElseCount() {
    elseCount++;
  }

  public void incrementIfCount() {
    ifCount++;
  }

  public void incrementLoopCount() {
    loopCount++;
  }

  public void incrementMethodCallCount() {
    methodCallCount++;
  }

  public void incrementPropertyCallCount() {
    propertyCallCount++;
  }

  public void incrementReturnCount() {
    returnCount++;
  }

  public void incrementSwitchCount() {
    switchCount++;
  }

  public void incrementVariableCount() {
    variableCount++;
  }

  // Getters
  public String getPackageName() {
    return packageName;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public int getArgCount() {
    return argCount;
  }

  // Setters
  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public void setArgCount(int count) {
    argCount = count;
  }

  public int getElseCount() {
    return elseCount;
  }

  public int getIfCount() {
    return ifCount;
  }

  public int getIfNestingLevel() {
    return ifNestingLevel;
  }

  public void setIfNestingLevel(int ifNestingLevel) {
    this.ifNestingLevel = ifNestingLevel;
  }

  public int getLineCount() {
    return lineCount;
  }

  public void setLineCount(int lineCount) {
    this.lineCount = lineCount;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  public int getLoopCount() {
    return loopCount;
  }

  public int getMethodCallCount() {
    return methodCallCount;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public int getPropertyCallCount() {
    return propertyCallCount;
  }

  public int getReturnCount() {
    return returnCount;
  }

  public int getSwitchCount() {
    return switchCount;
  }

  public int getTryCatchNestingLevel() {
    return tryCatchNestingLevel;
  }

  public void setTryCatchNestingLevel(int tryCatchNestingLevel) {
    this.tryCatchNestingLevel = tryCatchNestingLevel;
  }

  public int getVariableCount() {
    return variableCount;
  }

  public double getCognitiveComplexity() {
    return cognitiveComplexity;
  }

  public void setCognitiveComplexity(double cognitiveComplexity) {
    this.cognitiveComplexity = cognitiveComplexity;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public double getIfCountWeight() {
    return ifCountWeight;
  }

  public void setIfCountWeight(double ifCountWeight) {
    this.ifCountWeight = ifCountWeight;
  }

  public double getElseCountWeight() {
    return elseCountWeight;
  }

  public void setElseCountWeight(double elseCountWeight) {
    this.elseCountWeight = elseCountWeight;
  }

  public double getLoopCountWeight() {
    return loopCountWeight;
  }

  public void setLoopCountWeight(double loopCountWeight) {
    this.loopCountWeight = loopCountWeight;
  }

  public double getSwitchCountWeight() {
    return switchCountWeight;
  }

  public void setSwitchCountWeight(double switchCountWeight) {
    this.switchCountWeight = switchCountWeight;
  }

  public double getMethodCallCountWeight() {
    return methodCallCountWeight;
  }

  public void setMethodCallCountWeight(double methodCallCountWeight) {
    this.methodCallCountWeight = methodCallCountWeight;
  }

  public double getReturnCountWeight() {
    return returnCountWeight;
  }

  public void setReturnCountWeight(double returnCountWeight) {
    this.returnCountWeight = returnCountWeight;
  }

  public double getArgCountWeight() {
    return argCountWeight;
  }

  public void setArgCountWeight(double argCountWeight) {
    this.argCountWeight = argCountWeight;
  }

  public double getLineNumberWeight() {
    return lineNumberWeight;
  }

  public void setLineNumberWeight(double lineNumberWeight) {
    this.lineNumberWeight = lineNumberWeight;
  }

  public double getIfNestingLevelWeight() {
    return ifNestingLevelWeight;
  }

  public void setIfNestingLevelWeight(double ifNestingLevelWeight) {
    this.ifNestingLevelWeight = ifNestingLevelWeight;
  }

  public double getTryCatchNestingLevelWeight() {
    return tryCatchNestingLevelWeight;
  }

  public void setTryCatchNestingLevelWeight(double tryCatchNestingLevelWeight) {
    this.tryCatchNestingLevelWeight = tryCatchNestingLevelWeight;
  }

  public double getLineCountWeight() {
    return lineCountWeight;
  }

  public void setLineCountWeight(double lineCountWeight) {
    this.lineCountWeight = lineCountWeight;
  }

  public double getVariableCountWeight() {
    return variableCountWeight;
  }

  public void setVariableCountWeight(double variableCountWeight) {
    this.variableCountWeight = variableCountWeight;
  }

  public double getPropertyCallCountWeight() {
    return propertyCallCountWeight;
  }

  public void setPropertyCallCountWeight(double propertyCallCountWeight) {
    this.propertyCallCountWeight = propertyCallCountWeight;
  }

  public Delta getIfCountDelta() {
    return ifCountDelta;
  }

  public Delta getElseCountDelta() {
    return elseCountDelta;
  }

  public Delta getLoopCountDelta() {
    return loopCountDelta;
  }

  public Delta getSwitchCountDelta() {
    return switchCountDelta;
  }

  public Delta getMethodCallCountDelta() {
    return methodCallCountDelta;
  }

  public Delta getReturnCountDelta() {
    return returnCountDelta;
  }

  public Delta getArgCountDelta() {
    return argCountDelta;
  }

  public Delta getLineNumberDelta() {
    return lineNumberDelta;
  }

  public Delta getIfNestingLevelDelta() {
    return ifNestingLevelDelta;
  }

  public Delta getTryCatchNestingLevelDelta() {
    return tryCatchNestingLevelDelta;
  }

  public Delta getLineCountDelta() {
    return lineCountDelta;
  }

  public Delta getVariableCountDelta() {
    return variableCountDelta;
  }

  public Delta getPropertyCallCountDelta() {
    return propertyCallCountDelta;
  }

  public void setIfCountDelta(Delta ifCountDelta) {
    this.ifCountDelta = ifCountDelta;
  }

  public void setElseCountDelta(Delta elseCountDelta) {
    this.elseCountDelta = elseCountDelta;
  }

  public void setLoopCountDelta(Delta loopCountDelta) {
    this.loopCountDelta = loopCountDelta;
  }

  public void setSwitchCountDelta(Delta switchCountDelta) {
    this.switchCountDelta = switchCountDelta;
  }

  public void setMethodCallCountDelta(Delta methodCallCountDelta) {
    this.methodCallCountDelta = methodCallCountDelta;
  }

  public void setReturnCountDelta(Delta returnCountDelta) {
    this.returnCountDelta = returnCountDelta;
  }

  public void setArgCountDelta(Delta argCountDelta) {
    this.argCountDelta = argCountDelta;
  }

  public void setLineNumberDelta(Delta lineNumberDelta) {
    this.lineNumberDelta = lineNumberDelta;
  }

  public void setIfNestingLevelDelta(Delta ifNestingLevelDelta) {
    this.ifNestingLevelDelta = ifNestingLevelDelta;
  }

  public void setTryCatchNestingLevelDelta(Delta tryCatchNestingLevelDelta) {
    this.tryCatchNestingLevelDelta = tryCatchNestingLevelDelta;
  }

  public void setLineCountDelta(Delta lineCountDelta) {
    this.lineCountDelta = lineCountDelta;
  }

  public void setVariableCountDelta(Delta variableCountDelta) {
    this.variableCountDelta = variableCountDelta;
  }

  public void setPropertyCallCountDelta(Delta propertyCallCountDelta) {
    this.propertyCallCountDelta = propertyCallCountDelta;
  }

  public void calculateDeltas(CognitiveMetrics previousMetrics) {
    ifCountDelta = new Delta(previousMetrics.getIfCountWeight(), ifCountWeight);
    elseCountDelta = new Delta(previousMetrics.getElseCountWeight(), elseCountWeight);
    loopCountDelta = new Delta(previousMetrics.getLoopCountWeight(), loopCountWeight);
    switchCountDelta = new Delta(previousMetrics.getSwitchCountWeight(), switchCountWeight);
    methodCallCountDelta = new Delta(previousMetrics.getMethodCallCountWeight(), methodCallCountWeight);
    returnCountDelta = new Delta(previousMetrics.getReturnCountWeight(), returnCountWeight);
    argCountDelta = new Delta(previousMetrics.getArgCountWeight(), argCountWeight);
    lineNumberDelta = new Delta(previousMetrics.getLineNumberWeight(), lineNumberWeight);
    ifNestingLevelDelta = new Delta(previousMetrics.getIfNestingLevelWeight(), ifNestingLevelWeight);
    tryCatchNestingLevelDelta = new Delta(previousMetrics.getTryCatchNestingLevelWeight(), tryCatchNestingLevelWeight);
    lineCountDelta = new Delta(previousMetrics.getLineCountWeight(), lineCountWeight);
    variableCountDelta = new Delta(previousMetrics.getVariableCountWeight(), variableCountWeight);
    propertyCallCountDelta = new Delta(previousMetrics.getPropertyCallCountWeight(), propertyCallCountWeight);
  }
}
