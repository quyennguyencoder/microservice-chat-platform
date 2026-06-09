package com.nguyenquyen.conversationservice.exception;

/**
 * Thrown when a chat cannot be found by its UUID.
 *
 * <p>Mapped to HTTP {@code 404 Not Found} by {@link GlobalExceptionHandler}.</p>
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
public class ChatNotFoundException extends RuntimeException {

    public ChatNotFoundException(String message) {
        super(message);
    }
}
