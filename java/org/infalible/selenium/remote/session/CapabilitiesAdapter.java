package org.infalible.selenium.remote.session;

import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
public interface CapabilitiesAdapter extends Function<Map<String, Object>, Map<String, Object>> {
}
