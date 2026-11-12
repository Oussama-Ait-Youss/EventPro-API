package com.eventpro.api.exception;

public class NonClientReservationException extends RuntimeException{
    public NonClientReservationException(String message){
        super(message);
    }
}
