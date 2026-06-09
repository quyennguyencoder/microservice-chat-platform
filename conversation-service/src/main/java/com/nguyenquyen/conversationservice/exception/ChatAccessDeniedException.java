package com.nguyenquyen.conversationservice.exception;

/**
 * Thrown when a user attempts to access or perform an action on a chat
 * they are not a participant of.
 *
 * <p>Mapped to HTTP {@code 403 Forbidden} by {@link GlobalExceptionHandler}.</p>
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
public class ChatAccessDeniedException extends RuntimeException {

    public ChatAccessDeniedException(String message) {
        super(message);
    }
}
