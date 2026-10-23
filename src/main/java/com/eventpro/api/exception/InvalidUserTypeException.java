package com.eventpro.api.exception;

public class InvalidUserTypeException extends RuntimeException{
    public InvalidUserTypeException(String message){
        super(message);
    }
}
