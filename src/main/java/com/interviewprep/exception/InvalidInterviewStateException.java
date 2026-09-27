package com.interviewprep.exception;

public class InvalidInterviewStateException extends RuntimeException {
    public InvalidInterviewStateException(String currentState, String requiredState) {
        super(String.format("Invalid interview state. Current: %s, Required: %s", currentState, requiredState));
    }
}
