/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.watch.webhook.websocket;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;

import static java.lang.System.getLogger;


/**
 * WebSocketNotification.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/06 umjammer initial version <br>
 */
public abstract class WebSocketNotification<T> extends BaseWebSocketNotification<T> {

    private static final Logger logger = getLogger(WebSocketNotification.class.getName());

    /** */
    protected WebSocketNotification(URI uri, Object... args) throws IOException {
        super(uri, args);
    }

    @OnOpen
    public final void onOpen(Session session) throws IOException {
logger.log(Level.DEBUG, "WEBSOCKET: onOpen: " + session.getId());
        onOpenImpl(session);
    }

    @OnMessage
    public final void onNotifyMessage(T notification, Session session) throws IOException {
        onNotifyMessageImpl(notification);
    }

    @OnClose
    public final void onClose(Session session) throws IOException {
        onCloseInternal(session);
    }
}
