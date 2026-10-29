package com.eventpro.api.exception;

public class ReservationExceedsEventCapacityException extends RuntimeException{
    public ReservationExceedsEventCapacityException(String message){
        super(message);
    }
}
