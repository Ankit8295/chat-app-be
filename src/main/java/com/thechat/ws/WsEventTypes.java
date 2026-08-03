package com.thechat.ws;

public final class WsEventTypes {

    public static final String READY = "ready";
    public static final String PING = "ping";
    public static final String PONG = "pong";
    public static final String ERROR = "error";

    public static final String MESSAGE_SEND = "message_send"; // client send message under this key
    public static final String MESSAGE_NEW = "message_new";// clients recieve message under this key

    public static final String MESSAGE_UPDATE = "message_update"; // client send update message under this key
    public static final String MESSAGE_UPDATED = "message_updated"; // clients recieve updated message under this key

    public static final String MESSAGE_DELETE = "message_delete"; // client send delete message under this key
    public static final String MESSAGE_DELETED = "message_deleted"; // clients recieve deleted message under this key

    private WsEventTypes() {
    }
}
