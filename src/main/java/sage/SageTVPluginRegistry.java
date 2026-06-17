package sage;

import java.util.Map;

public interface SageTVPluginRegistry {
    void eventSubscribe(SageTVEventListener listener, String eventName);

    void eventUnsubscribe(SageTVEventListener listener, String eventName);

    void postEvent(String eventName, Map eventVars);

    void postEvent(String eventName, Map eventVars, boolean waitUntilDone);
}
