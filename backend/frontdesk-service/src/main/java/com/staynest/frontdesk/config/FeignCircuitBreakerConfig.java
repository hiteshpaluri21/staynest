package com.staynest.frontdesk.config;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;

import java.time.Duration;

/**
 * Tunes the resilience4j circuit breaker that backs the Feign {@code fallbackFactory}
 * clients so it stays safe alongside the existing cross-service call handling:
 *
 * <ul>
 *   <li><b>Runs on the calling thread.</b> By default Spring Cloud CircuitBreaker
 *   enforces its time limiter on a separate thread pool, which would drop the
 *   {@code RequestContextHolder} ThreadLocal that {@link FeignClientConfig} relies on
 *   to relay the caller's {@code Authorization} header. A synchronous executor keeps
 *   the call on the request thread so JWT propagation keeps working.</li>
 *   <li><b>Ignores 4xx.</b> {@code 404 Not Found} / {@code 400 Bad Request} are
 *   business outcomes (e.g. "no such reservation"), not availability failures — so they
 *   never trip the breaker and propagate unchanged to the callers' existing fail-closed
 *   validation logic.</li>
 * </ul>
 */
@Configuration
public class FeignCircuitBreakerConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> feignCircuitBreakerCustomizer() {
        return factory -> {
            factory.configureExecutorService(new ExecutorServiceAdapter(new SyncTaskExecutor()));
            factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(CircuitBreakerConfig.custom()
                            .ignoreExceptions(FeignException.NotFound.class, FeignException.BadRequest.class)
                            .build())
                    .timeLimiterConfig(TimeLimiterConfig.custom()
                            .timeoutDuration(Duration.ofSeconds(10))
                            .build())
                    .build());
        };
    }
}
