package com.tesladodger.neat;


/**
 * Thrown when a method is called from the wrong mode.
 */
class InvalidModeException extends RuntimeException {

    InvalidModeException (String s) {
        super(s);
    }

}
