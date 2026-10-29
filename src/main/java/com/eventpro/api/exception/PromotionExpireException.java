package com.eventpro.api.exception;

public class PromotionExpireException extends RuntimeException{
    public PromotionExpireException(String message){
        super(message);
    }
}
