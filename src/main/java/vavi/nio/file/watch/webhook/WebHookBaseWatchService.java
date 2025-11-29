/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.watch.webhook;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.Watchable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.github.fge.filesystem.watch.AbstractWatchService;

import static java.lang.System.getLogger;


/**
 * WebHookBaseWatchService.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/06 umjammer initial version <br>
 */
public abstract class WebHookBaseWatchService<T> extends AbstractWatchService {

    private static final Logger logger = getLogger(WebHookBaseWatchService.class.getName());

    /** */
    private static final Map<String, Notification<?>> notifications = new HashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(WebHookBaseWatchService::dispose));
    }

    /** */
    protected static <T> void setupNotification(WebHookBaseWatchService<T> o, String matcher, Object... args) throws IOException {
        if (!notifications.containsKey(matcher)) {
            Notification<T> notification = Notification.getNotification(matcher, n -> {
                try {
                    o.onNotifyMessage(n);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }, args);
            notifications.put(matcher, notification);
        }
    }

    /** call {@link #listener}.accept */
    protected abstract void onNotifyMessage(T notification) throws IOException;

    /** for the user watch service */
    protected BiConsumer<String, Kind<?>> listener = this::processNotification;

    /** for the system watch service */
    public void setNotificationListener(BiConsumer<String, Kind<?>> listener) {
        this.listener = listener;
    }

    /** for watchkey */
    private void processNotification(String id, Kind<?> kind) {
        for (BasicWatchKey watchKey : watchKeys) {
            if (watchKey.subscribesTo(kind)) {
                watchKey.signal();
            }
        }
    }

    /** TODO */
    public static void dispose() {
        notifications.values().forEach(notification -> {
            try {
                notification.close();
logger.log(Level.DEBUG, "NOTIFICATION: notification closed: " + notification);
            } catch (IOException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        });
        notifications.clear();
    }

    /** */
    private final List<BasicWatchKey> watchKeys = new ArrayList<>();

    // design error?
    @Override
    public BasicWatchKey register(Watchable watchable,
                                  Iterable<? extends WatchEvent.Kind<?>> eventTypes,
                                  WatchEvent.Modifier... modifiers) throws IOException {
//        if (eventTypes == null) {
//            throw new IllegalArgumentException("no eventTypes");
//        }
        BasicWatchKey watchKey = super.register(watchable, eventTypes);
        watchKeys.add(watchKey);
        return watchKey;
    }

    @Override
    public void cancelled(BasicWatchKey watchKey) {
        watchKeys.remove(watchKey); // TODO ???
    }
}
