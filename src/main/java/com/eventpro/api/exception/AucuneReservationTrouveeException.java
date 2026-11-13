package com.eventpro.api.exception;


//==========================================================
//Mise en situation 1 : Requête JPQL, Exception & Controller
//==========================================================


public class AucuneReservationTrouveeException extends RuntimeException{
    public AucuneReservationTrouveeException(String message){
        super(message);
    }
}
