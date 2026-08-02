package com.thechat.ws;

public final class WsEventTypes {

    public static final String READY = "ready";
    public static final String PING = "ping";
    public static final String PONG = "pong";
    public static final String ERROR = "error";

    public static final String MESSAGE_SEND = "message_send";
    public static final String MESSAGE_NEW = "message_new";
    public static final String MESSAGE_UPDATED = "message_updated";
    public static final String MESSAGE_DELETED = "message_deleted";

    private WsEventTypes() {
    }
}
