package com.nguyenquyen.statusservice.entity;


public enum Status {

    /** User is active and connected. Redis key has a 5-min TTL. */
    ONLINE,

    /** User has explicitly signed off or session expired. */
    OFFLINE,

    /** User is logged in but not actively using the app. */
    AWAY,

    /** User is logged in but does not want to be disturbed. */
    BUSY
}
