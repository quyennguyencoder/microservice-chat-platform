package com.nguyenquyen.contactservice.enums;

/**
 * Represents the lifecycle state of a contact relationship.
 *
 * <ul>
 *   <li>{@link #PENDING}  — request sent by requester, awaiting addressee's response</li>
 *   <li>{@link #ACCEPTED} — both users are contacts</li>
 * </ul>
 *
 * @author  Aniket Kamlesh
 * @version 1.0.0
 * @since   1.0.0
 */
public enum ContactStatus {

    /**
     * Contact request has been sent and is awaiting acceptance.
     */
    PENDING,

    /**
     * Contact request has been accepted — both users are contacts.
     */
    ACCEPTED
}
