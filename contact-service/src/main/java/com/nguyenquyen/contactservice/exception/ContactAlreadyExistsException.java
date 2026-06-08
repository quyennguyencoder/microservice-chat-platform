package com.nguyenquyen.contactservice.exception;

/**
 * Thrown when a contact request already exists between two users
 * (in PENDING or ACCEPTED status).
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
public class ContactAlreadyExistsException extends RuntimeException {

    public ContactAlreadyExistsException(String message) {
        super(message);
    }
}
