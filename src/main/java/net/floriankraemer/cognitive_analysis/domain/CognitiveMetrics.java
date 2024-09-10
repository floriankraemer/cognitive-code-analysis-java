package net.floriankraemer.cognitive_analysis.domain;

import org.springframework.stereotype.Component;

/**
 * A data object that holds cognitive metrics.
 */
@Component
public class CognitiveMetrics {

  private String file;
  private String className;
  private String methodName;

  private int ifCount = 0;
  private double ifCountWeight = 0.0;

  private int elseCount = 0;
  private double elseCountWeight = 0.0;

  private int loopCount = 0;
  private double loopCountWeight = 0.0;

  private int switchCount = 0;
  private double switchCountWeight = 0.0;

  private int methodCallCount = 0;
  private double methodCallCountWeight = 0.0;

  private int returnCount = 0;
  private double returnCountWeight = 0.0;

  private int argumentCount = 0;
  private double argumentCountWeight = 0.0;

  private int lineNumber = 0;
  private double lineNumberWeight = 0.0;

  private int ifNestingLevel = 0;
  private double ifNestingLevelWeight = 0.0;

  private int tryCatchNestingLevel = 0;
  private double tryCatchNestingLevelWeight = 0.0;

  private int lineCount = 0;
  private double lineCountWeight = 0.0;

  private int variableCount = 0;
  private double variableCountWeight = 0.0;

  private int fieldAccessCount = 0;
  private double propertyCallCountWeight = 0.0;

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

  // Getters and Setters for ifCountWeight
  public double getIfCountWeight() {
    return ifCountWeight;
  }

  public void setIfCountWeight(double ifCountWeight) {
    this.ifCountWeight = ifCountWeight;
  }

  // Getters and Setters for elseCountWeight
  public double getElseCountWeight() {
    return elseCountWeight;
  }

  public void setElseCountWeight(double elseCountWeight) {
    this.elseCountWeight = elseCountWeight;
  }

  // Getters and Setters for loopCountWeight
  public double getLoopCountWeight() {
    return loopCountWeight;
  }

  public void setLoopCountWeight(double loopCountWeight) {
    this.loopCountWeight = loopCountWeight;
  }

  // Getters and Setters for switchCountWeight
  public double getSwitchCountWeight() {
    return switchCountWeight;
  }

  public void setSwitchCountWeight(double switchCountWeight) {
    this.switchCountWeight = switchCountWeight;
  }

  // Getters and Setters for methodCallCountWeight
  public double getMethodCallCountWeight() {
    return methodCallCountWeight;
  }

  public void setMethodCallCountWeight(double methodCallCountWeight) {
    this.methodCallCountWeight = methodCallCountWeight;
  }

  // Getters and Setters for returnCountWeight
  public double getReturnCountWeight() {
    return returnCountWeight;
  }

  public void setReturnCountWeight(double returnCountWeight) {
    this.returnCountWeight = returnCountWeight;
  }

  // Getters and Setters for argumentCountWeight
  public double getArgumentCountWeight() {
    return argumentCountWeight;
  }

  public void setArgumentCountWeight(double argumentCountWeight) {
    this.argumentCountWeight = argumentCountWeight;
  }

  // Getters and Setters for lineNumberWeight
  public double getLineNumberWeight() {
    return lineNumberWeight;
  }

  public void setLineNumberWeight(double lineNumberWeight) {
    this.lineNumberWeight = lineNumberWeight;
  }

  // Getters and Setters for ifNestingLevelWeight
  public double getIfNestingLevelWeight() {
    return ifNestingLevelWeight;
  }

  public void setIfNestingLevelWeight(double ifNestingLevelWeight) {
    this.ifNestingLevelWeight = ifNestingLevelWeight;
  }

  // Getters and Setters for tryCatchNestingLevelWeight
  public double getTryCatchNestingLevelWeight() {
    return tryCatchNestingLevelWeight;
  }

  public void setTryCatchNestingLevelWeight(double tryCatchNestingLevelWeight) {
    this.tryCatchNestingLevelWeight = tryCatchNestingLevelWeight;
  }

  // Getters and Setters for lineCountWeight
  public double getLineCountWeight() {
    return lineCountWeight;
  }

  public void setLineCountWeight(double lineCountWeight) {
    this.lineCountWeight = lineCountWeight;
  }

  // Getters and Setters for variableCountWeight
  public double getVariableCountWeight() {
    return variableCountWeight;
  }

  public void setVariableCountWeight(double variableCountWeight) {
    this.variableCountWeight = variableCountWeight;
  }

  // Getters and Setters for propertyCallCountWeight
  public double getPropertyCallCountWeight() {
    return propertyCallCountWeight;
  }

  public void setPropertyCallCountWeight(double propertyCallCountWeight) {
    this.propertyCallCountWeight = propertyCallCountWeight;
  }

  // Override toString
  @Override
  public String toString() {
    return "CognitiveMetrics{"
        + "file='" + file + '\''
        + ", className='" + className + '\''
        + ", methodName='" + methodName + '\''
        + ", ifCount=" + ifCount
        + ", elseCount=" + elseCount
        + ", loopCount=" + loopCount
        + ", switchCount=" + switchCount
        + ", methodCallCount=" + methodCallCount
        + ", returnCount=" + returnCount
        + ", argumentCount=" + argumentCount
        + ", lineNumber=" + lineNumber
        + ", lineCount=" + lineCount
        + ", variableCount=" + variableCount
        + ", propertyCallCount=" + fieldAccessCount
        + '}';
  }
}
