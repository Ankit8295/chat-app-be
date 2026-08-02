package com.thechat.ws;

public final class WsEventTypes {

    public static final String READY = "ready";
    public static final String PING = "ping";
    public static final String PONG = "pong";
    public static final String ERROR = "error";

    public static final String MESSAGE_SEND = "message_send";
    public static final String MESSAGE_NEW = "message_new";

    public static final String MESSAGE_UPDATE = "message_update";
    public static final String MESSAGE_UPDATED = "message_updated";

    public static final String MESSAGE_DELETE = "message_delete";
    public static final String MESSAGE_DELETED = "message_deleted";

    public static final String MEMBER_ADD = "member_add";
    public static final String MEMBER_ADDED = "member_added";

    public static final String MEMBER_REMOVE = "member_remove";
    public static final String MEMBER_REMOVED = "member_removed";

    public static final String GROUP_UPDATE = "group_update";
    public static final String GROUP_UPDATED = "group_updated";

    private WsEventTypes() {
    }
}
