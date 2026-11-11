package com.yolda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SelfActionException extends RuntimeException {

    public SelfActionException(String message) {
        super(message);
    }
}
