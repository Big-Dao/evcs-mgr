package com.evcs.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalApiRouteGuardRunnerTest {

    @Test
    @DisplayName("启动期路由校验 - 暴露 /internal/api/** 时应 fail-fast")
    void testRun_shouldThrow_whenRouteExposesInternalApi() {
        // Arrange
        InternalApiRouteGuardProperties properties = new InternalApiRouteGuardProperties();
        properties.setEnabled(true);
        properties.setStartupTimeout(Duration.ofSeconds(5));
        properties.setForbiddenPathPrefixes(List.of("/internal/api/"));

        RouteDefinition badRoute = new RouteDefinition();
        badRoute.setId("bad-route");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        Map<String, String> args = new HashMap<>();
        args.put("_genkey_0", "/internal/api/**");
        pathPredicate.setArgs(args);

        badRoute.setPredicates(List.of(pathPredicate));

        RouteDefinitionLocator locator = () -> Flux.just(badRoute);
        InternalApiRouteGuardRunner runner = new InternalApiRouteGuardRunner(locator, properties);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> runner.run(null),
                "暴露 internal api 的路由应导致启动失败");
    }

    @Test
    @DisplayName("启动期路由校验 - 非 internal 路由应通过")
    void testRun_shouldPass_whenRouteDoesNotExposeInternalApi() {
        // Arrange
        InternalApiRouteGuardProperties properties = new InternalApiRouteGuardProperties();
        properties.setEnabled(true);
        properties.setStartupTimeout(Duration.ofSeconds(5));
        properties.setForbiddenPathPrefixes(List.of("/internal/api/"));

        RouteDefinition okRoute = new RouteDefinition();
        okRoute.setId("ok-route");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        Map<String, String> args = new HashMap<>();
        args.put("_genkey_0", "/api/auth/**");
        pathPredicate.setArgs(args);

        okRoute.setPredicates(List.of(pathPredicate));

        RouteDefinitionLocator locator = () -> Flux.just(okRoute);
        InternalApiRouteGuardRunner runner = new InternalApiRouteGuardRunner(locator, properties);

        // Act & Assert
        assertDoesNotThrow(() -> runner.run(null), "非 internal 路由不应被拦截");
    }

    @Test
    @DisplayName("启动期路由校验 - 逗号分隔 patterns 中包含 internal 时应 fail-fast")
    void testRun_shouldThrow_whenCommaSeparatedContainsInternalApi() {
        // Arrange
        InternalApiRouteGuardProperties properties = new InternalApiRouteGuardProperties();
        properties.setEnabled(true);
        properties.setStartupTimeout(Duration.ofSeconds(5));
        properties.setForbiddenPathPrefixes(List.of("/internal/api/"));

        RouteDefinition badRoute = new RouteDefinition();
        badRoute.setId("comma-route");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        Map<String, String> args = new HashMap<>();
        args.put("_genkey_0", "/api/foo/**, /internal/api/**");
        pathPredicate.setArgs(args);

        badRoute.setPredicates(List.of(pathPredicate));

        RouteDefinitionLocator locator = () -> Flux.just(badRoute);
        InternalApiRouteGuardRunner runner = new InternalApiRouteGuardRunner(locator, properties);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> runner.run(null),
                "逗号分隔 patterns 只要包含 internal api 也应导致启动失败");
    }

    @Test
    @DisplayName("启动期路由校验 - enabled=false 时应跳过校验")
    void testRun_shouldSkip_whenDisabled() {
        // Arrange
        InternalApiRouteGuardProperties properties = new InternalApiRouteGuardProperties();
        properties.setEnabled(false);

        RouteDefinition badRoute = new RouteDefinition();
        badRoute.setId("bad-route");

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        Map<String, String> args = new HashMap<>();
        args.put("_genkey_0", "/internal/api/**");
        pathPredicate.setArgs(args);

        badRoute.setPredicates(List.of(pathPredicate));

        RouteDefinitionLocator locator = () -> Flux.just(badRoute);
        InternalApiRouteGuardRunner runner = new InternalApiRouteGuardRunner(locator, properties);

        // Act & Assert
        assertDoesNotThrow(() -> runner.run(null), "禁用时不应 fail-fast");
    }
}
