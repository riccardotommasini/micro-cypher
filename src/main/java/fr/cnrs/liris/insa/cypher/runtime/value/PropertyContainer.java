package fr.cnrs.liris.insa.cypher.runtime.value;

import java.util.Map;

public interface PropertyContainer {

    Map<String, Object> properties();

    default Object property(String key) {
        return properties().get(key);
    }
}
