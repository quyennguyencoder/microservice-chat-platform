package com.nguyenquyen.contactservice.exception;

/**
 * Thrown when a user attempts to modify a contact record they do not own.
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
public class ContactAccessDeniedException extends RuntimeException {

    public ContactAccessDeniedException(String message) {
        super(message);
    }
}
