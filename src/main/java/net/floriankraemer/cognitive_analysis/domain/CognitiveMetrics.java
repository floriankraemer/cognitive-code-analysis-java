package net.floriankraemer.cognitive_analysis.domain;

import org.springframework.stereotype.Component;

@Component
public class CognitiveMetrics {

  private String file;
  private String className;
  private String methodName;
  private String packageName;

  private int ifCount = 0;
  private double ifCountWeight = 0.0;
  private Delta ifCountDelta = null;

  private int elseCount = 0;
  private double elseCountWeight = 0.0;
  private Delta elseCountDelta = null;

  private int loopCount = 0;
  private double loopCountWeight = 0.0;
  private Delta loopCountDelta = null;

  private int switchCount = 0;
  private double switchCountWeight = 0.0;
  private Delta switchCountDelta = null;

  private int methodCallCount = 0;
  private double methodCallCountWeight = 0.0;
  private Delta methodCallCountDelta = null;

  private int returnCount = 0;
  private double returnCountWeight = 0.0;
  private Delta returnCountDelta = null;

  private int argumentCount = 0;
  private double argumentCountWeight = 0.0;
  private Delta argumentCountDelta = null;

  private int lineNumber = 0;
  private double lineNumberWeight = 0.0;
  private Delta lineNumberDelta = null;

  private int ifNestingLevel = 0;
  private double ifNestingLevelWeight = 0.0;
  private Delta ifNestingLevelDelta = null;

  private int tryCatchNestingLevel = 0;
  private double tryCatchNestingLevelWeight = 0.0;
  private Delta tryCatchNestingLevelDelta = null;

  private int lineCount = 0;
  private double lineCountWeight = 0.0;
  private Delta lineCountDelta = null;

  private int variableCount = 0;
  private double variableCountWeight = 0.0;
  private Delta variableCountDelta = null;

  private int fieldAccessCount = 0;
  private double propertyCallCountWeight = 0.0;
  private Delta propertyCallCountDelta = null;

  private double cognitiveComplexity = 0;

  public CognitiveMetrics() {
  }

  public CognitiveMetrics(String className, String methodName) {
    this.className = className;
    this.methodName = methodName;
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

  public void incrementFieldAccessCount() {
    fieldAccessCount++;
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

  public int getArgumentCount() {
    return argumentCount;
  }

  // Setters
  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public void setArgumentCount(int count) {
    argumentCount = count;
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

  public int getFieldAccessCount() {
    return fieldAccessCount;
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

  public double getArgumentCountWeight() {
    return argumentCountWeight;
  }

  public void setArgumentCountWeight(double argumentCountWeight) {
    this.argumentCountWeight = argumentCountWeight;
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

  public Delta getArgumentCountDelta() {
    return argumentCountDelta;
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

  public void setArgumentCountDelta(Delta argumentCountDelta) {
    this.argumentCountDelta = argumentCountDelta;
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
    argumentCountDelta = new Delta(previousMetrics.getArgumentCountWeight(), argumentCountWeight);
    lineNumberDelta = new Delta(previousMetrics.getLineNumberWeight(), lineNumberWeight);
    ifNestingLevelDelta = new Delta(previousMetrics.getIfNestingLevelWeight(), ifNestingLevelWeight);
    tryCatchNestingLevelDelta = new Delta(previousMetrics.getTryCatchNestingLevelWeight(), tryCatchNestingLevelWeight);
    lineCountDelta = new Delta(previousMetrics.getLineCountWeight(), lineCountWeight);
    variableCountDelta = new Delta(previousMetrics.getVariableCountWeight(), variableCountWeight);
    propertyCallCountDelta = new Delta(previousMetrics.getPropertyCallCountWeight(), propertyCallCountWeight);
  }

  // Override toString
  @Override
  public String toString() {
    return "CognitiveMetrics{"
        + "file='" + file + '\''
        + ", className='" + className + '\''
        + ", methodName='" + methodName + '\''
        + ", packageName='" + packageName + '\''
        + ", ifCount=" + ifCount
        + ", ifCountWeight=" + ifCountWeight
        + ", ifCountDelta=" + ifCountDelta
        + ", elseCount=" + elseCount
        + ", elseCountWeight=" + elseCountWeight
        + ", elseCountDelta=" + elseCountDelta
        + ", loopCount=" + loopCount
        + ", loopCountWeight=" + loopCountWeight
        + ", loopCountDelta=" + loopCountDelta
        + ", switchCount=" + switchCount
        + ", switchCountWeight=" + switchCountWeight
        + ", switchCountDelta=" + switchCountDelta
        + ", methodCallCount=" + methodCallCount
        + ", methodCallCountWeight=" + methodCallCountWeight
        + ", methodCallCountDelta=" + methodCallCountDelta
        + ", returnCount=" + returnCount
        + ", returnCountWeight=" + returnCountWeight
        + ", returnCountDelta=" + returnCountDelta
        + ", argumentCount=" + argumentCount
        + ", argumentCountWeight=" + argumentCountWeight
        + ", argumentCountDelta=" + argumentCountDelta
        + ", lineNumber=" + lineNumber
        + ", lineNumberWeight=" + lineNumberWeight
        + ", lineNumberDelta=" + lineNumberDelta
        + ", ifNestingLevel=" + ifNestingLevel
        + ", ifNestingLevelWeight=" + ifNestingLevelWeight
        + ", ifNestingLevelDelta=" + ifNestingLevelDelta
        + ", tryCatchNestingLevel=" + tryCatchNestingLevel
        + ", tryCatchNestingLevelWeight=" + tryCatchNestingLevelWeight
        + ", tryCatchNestingLevelDelta=" + tryCatchNestingLevelDelta
        + ", lineCount=" + lineCount
        + ", lineCountWeight=" + lineCountWeight
        + ", lineCountDelta=" + lineCountDelta
        + ", variableCount=" + variableCount
        + ", variableCountWeight=" + variableCountWeight
        + ", variableCountDelta=" + variableCountDelta
        + ", fieldAccessCount=" + fieldAccessCount
        + ", propertyCallCountWeight=" + propertyCallCountWeight
        + ", propertyCallCountDelta=" + propertyCallCountDelta
        + ", cognitiveComplexity=" + cognitiveComplexity
        + '}';
  }
}
