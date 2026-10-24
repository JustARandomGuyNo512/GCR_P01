package com.sheridan.gcr.modularSys.builder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ValidateResult {
    private Unit source;
    private final List<ValidationError> issues = new ArrayList<>();

    public ValidateResult() {}

    public ValidateResult(Unit source) {
        this.source = source;
    }

    void setSource(Unit source) {
        this.source = source;
    }

    void clear() {
        issues.clear();
    }

    public void recordError(List<Unit> targets, String message) {
        for (Unit unit : targets){
            recordError(unit, message);
        }
    }

    public void recordError(Unit target, String message) {
        issues.add(new ValidationError(source, target, message, ErrorLevel.ERROR));
    }

    public void recordError(String message) {
        issues.add(new ValidationError(source, null, message, ErrorLevel.ERROR));
    }

    public void recordWarning(String message) {
        issues.add(new ValidationError(source, null, message, ErrorLevel.WARNING));
    }

    public List<ValidationError> getIssues() {
        return List.copyOf(issues);
    }

    public List<ValidationError> getErrors() {
        return issues.stream().filter(e -> e.getLevel() == ErrorLevel.ERROR).toList();
    }

    public List<ValidationError> getWarnings() {
        return issues.stream().filter(e -> e.getLevel() == ErrorLevel.WARNING).toList();
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(e -> e.getLevel() == ErrorLevel.ERROR);
    }

    public boolean isCommitAllowed() {
        return !hasErrors();
    }


    public void sortIssues(boolean reverse) {
        if (reverse) {
            issues.sort(Comparator.comparing(ValidationError::getLevel).reversed());
        } else {
            issues.sort(Comparator.comparing(ValidationError::getLevel));
        }
    }

    public ValidateResult and(ValidateResult other) {
        issues.addAll(other.issues);
        return this;
    }
}
