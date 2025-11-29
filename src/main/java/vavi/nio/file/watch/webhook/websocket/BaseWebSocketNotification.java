/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.watch.webhook.websocket;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnError;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import vavi.nio.file.watch.webhook.Notification;

import static java.lang.System.getLogger;


/**
 * BaseWebSocketNotification.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/26 umjammer initial version <br>
 */
public abstract class BaseWebSocketNotification<T> implements Notification<T> {

    private static final Logger logger = getLogger(BaseWebSocketNotification.class.getName());

    /** */
    private final WebSocketContainer container = ContainerProvider.getWebSocketContainer();

    /** websocket uri */
    private final URI uri;
    /** */
    protected Session session;
    /** */
    private final AtomicBoolean reconnect = new AtomicBoolean(true);
    /** */
    private Throwable throwable;
    /** */
    protected Object[] args;

    /** */
    protected BaseWebSocketNotification(URI uri, Object... args) throws IOException {
        this.uri = uri;
        this.args = args;
        try {
            session = container.connectToServer(this, uri);
        } catch (DeploymentException e) {
            throw new IOException(e);
        }
    }

    /** */
    protected abstract void onOpenImpl(Session session) throws IOException;

    /** */
    protected abstract void onNotifyMessageImpl(T notification) throws IOException;

    @OnError
    public final void onError(Throwable t) {
logger.log(Level.DEBUG, "WEBSOCKET: onError", t);
        throwable = t;
    }

    /** */
    protected abstract void onCloseImpl(Session session) throws IOException;

    /** */
    protected final void onCloseInternal(Session session) throws IOException {
logger.log(Level.DEBUG, "WEBSOCKET: onClose: " + session.getId());
        onCloseImpl(session);

        if (reconnect.get()) {
            if (throwable == null) {
                try {
logger.log(Level.DEBUG, "WEBSOCKET: reconnect");
                    this.session = container.connectToServer(this, uri);
                } catch (DeploymentException e) {
                    throw new IOException(e);
                }
            } else {
logger.log(Level.DEBUG, "WEBSOCKET: has error, exit");
            }
        }
    }

    @Override
    public final void close() throws IOException {
        if (reconnect.getAndSet(false)) {
            session.close();
logger.log(Level.DEBUG, "WEBSOCKET: close: " + session.getId());

            // TODO encapsulate into jsr356
            // https://stackoverflow.com/a/46472909/6102938
            if (container != null && container instanceof org.eclipse.jetty.util.component.LifeCycle) {
                try {
logger.log(Level.DEBUG, "WEBSOCKET: stopping jetty's websocket client");
                    ((org.eclipse.jetty.util.component.LifeCycle) container).stop();
logger.log(Level.DEBUG, "WEBSOCKET: jetty's websocket stopped: " + ((org.eclipse.jetty.util.component.LifeCycle) container).isStopped());
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        }
//logger.log(Level.TRACE, "close: exit: " + reconnect.get() + ", " + this.hashCode());
    }
}
