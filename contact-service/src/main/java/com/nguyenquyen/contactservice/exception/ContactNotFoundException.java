package com.nguyenquyen.contactservice.exception;

/**
 * Thrown when a contact record cannot be found by the given ID.
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException(String message) {
        super(message);
    }
}
