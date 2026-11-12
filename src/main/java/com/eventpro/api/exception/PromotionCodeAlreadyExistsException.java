package com.eventpro.api.exception;

public class PromotionCodeAlreadyExistsException extends RuntimeException{
    public PromotionCodeAlreadyExistsException(String message){
        super(message);
    }
}
