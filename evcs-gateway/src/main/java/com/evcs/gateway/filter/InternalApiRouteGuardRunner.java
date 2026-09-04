package com.evcs.gateway.filter;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InternalApiRouteGuardRunner implements ApplicationRunner {

    private final RouteDefinitionLocator routeDefinitionLocator;

    private final InternalApiRouteGuardProperties properties;

    public InternalApiRouteGuardRunner(RouteDefinitionLocator routeDefinitionLocator, InternalApiRouteGuardProperties properties) {
        this.routeDefinitionLocator = Objects.requireNonNull(routeDefinitionLocator);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        List<String> forbiddenPathPrefixes = normalizeForbiddenPathPrefixes(properties.getForbiddenPathPrefixes());
        if (forbiddenPathPrefixes.isEmpty()) {
            return;
        }

        Duration timeout = properties.getStartupTimeout();
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            timeout = Duration.ofSeconds(5);
        }

        List<RouteDefinition> routeDefinitions = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block(timeout);

        if (routeDefinitions == null) {
            throw new IllegalStateException("Failed to validate gateway routes: route definitions unavailable");
        }

        List<String> violations = new ArrayList<>();
        for (RouteDefinition routeDefinition : routeDefinitions) {
            if (routeDefinition == null) {
                continue;
            }

            String routeId = routeDefinition.getId();
            List<PredicateDefinition> predicates = routeDefinition.getPredicates();
            if (predicates == null || predicates.isEmpty()) {
                continue;
            }

            for (PredicateDefinition predicate : predicates) {
                if (predicate == null) {
                    continue;
                }

                if (!isPathPredicate(predicate.getName())) {
                    continue;
                }

                for (String pattern : extractPathPatterns(predicate.getArgs())) {
                    if (pattern == null || pattern.isBlank()) {
                        continue;
                    }

                    if (isForbiddenPathPattern(pattern, forbiddenPathPrefixes)) {
                        violations.add("routeId=" + routeId + ", pattern=" + pattern);
                    }
                }
            }
        }

        if (!violations.isEmpty()) {
            String message = "Gateway route exposure detected for internal endpoints. "
                    + "Routes must not expose /internal/api/**. "
                    + "Fix the route predicates, or disable the guard via evcs.gateway.security.internal-api-route-guard.enabled=false. "
                    + "Violations: " + String.join("; ", violations);
            throw new IllegalStateException(message);
        }
    }

    private static boolean isPathPredicate(String predicateName) {
        return predicateName != null && "Path".equalsIgnoreCase(predicateName);
    }

    private static List<String> extractPathPatterns(Map<String, String> args) {
        if (args == null || args.isEmpty()) {
            return List.of();
        }

        List<String> patterns = new ArrayList<>();
        for (String raw : args.values()) {
            if (raw == null) {
                continue;
            }

            String[] parts = raw.split(",");
            for (String part : parts) {
                String value = part == null ? null : part.trim();
                if (value != null && !value.isEmpty()) {
                    patterns.add(value);
                }
            }
        }

        return patterns;
    }

    private static List<String> normalizeForbiddenPathPrefixes(Collection<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String prefix : prefixes) {
            if (prefix == null) {
                continue;
            }

            String value = prefix.trim();
            if (value.isEmpty()) {
                continue;
            }

            if (!value.startsWith("/")) {
                value = "/" + value;
            }

            normalized.add(value);
        }

        return normalized;
    }

    private static boolean isForbiddenPathPattern(String pattern, List<String> forbiddenPathPrefixes) {
        String normalizedPattern = pattern.trim();
        if (!normalizedPattern.startsWith("/")) {
            normalizedPattern = "/" + normalizedPattern;
        }

        for (String forbiddenPrefix : forbiddenPathPrefixes) {
            if (forbiddenPrefix == null || forbiddenPrefix.isBlank()) {
                continue;
            }

            String normalizedForbidden = forbiddenPrefix.trim();
            if (!normalizedForbidden.startsWith("/")) {
                normalizedForbidden = "/" + normalizedForbidden;
            }

            if (normalizedPattern.equals(normalizedForbidden)) {
                return true;
            }

            if (normalizedForbidden.endsWith("/")) {
                if (normalizedPattern.startsWith(normalizedForbidden)) {
                    return true;
                }
            } else {
                if (normalizedPattern.startsWith(normalizedForbidden)) {
                    int len = normalizedForbidden.length();
                    if (normalizedPattern.length() == len) {
                        return true;
                    }
                    char next = normalizedPattern.charAt(len);
                    if (next == '/' || next == '*') {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
