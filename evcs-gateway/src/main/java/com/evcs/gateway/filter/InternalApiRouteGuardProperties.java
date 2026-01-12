package com.evcs.gateway.filter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class InternalApiRouteGuardProperties {

    private boolean enabled = true;

    private Duration startupTimeout = Duration.ofSeconds(5);

    private List<String> forbiddenPathPrefixes = new ArrayList<>(List.of("/internal/api/"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    public List<String> getForbiddenPathPrefixes() {
        return forbiddenPathPrefixes;
    }

    public void setForbiddenPathPrefixes(List<String> forbiddenPathPrefixes) {
        this.forbiddenPathPrefixes = forbiddenPathPrefixes;
    }
}
