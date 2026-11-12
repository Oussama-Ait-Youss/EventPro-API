package com.eventpro.api.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class PerformanceAspect {

    private static final Logger log =
            LoggerFactory.getLogger("PERFORMANCE_LOG");

    private static final long SLOW_THRESHOLD_MS = 500;

    // 1. Intercept service implementation methods
    @Pointcut(
            "execution(* com.eventpro.api.service.impl..*.*(..))"
    )
    public void serviceMethods() {
    }

    // 2. Intercept repository methods
    @Pointcut(
            "execution(* org.springframework.data.repository.Repository+.*(..))"
    )
    public void repositoryMethods() {
    }

    // 3. Monitor both layers
    @Around("serviceMethods() || repositoryMethods()")
    public Object measureExecutionTime(
            ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.nanoTime();

        String methodName =
                joinPoint.getSignature().toShortString();

        String layer = isRepositoryMethod(joinPoint)
                ? "REPOSITORY"
                : "SERVICE";

        String status = "SUCCESS";

        try {
            // Execute the original method
            return joinPoint.proceed();

        } catch (Throwable exception) {

            status = "FAILED";

            throw exception;

        } finally {

            long elapsedNanos =
                    System.nanoTime() - startTime;

            double durationMs =
                    elapsedNanos / 1_000_000.0;

            if (durationMs >= SLOW_THRESHOLD_MS) {

                log.warn(
                        "layer={} method={} durationMs={} status=SLOW outcome={}",
                        layer,
                        methodName,
                        String.format(
                                java.util.Locale.ROOT,
                                "%.2f",
                                durationMs
                        ),
                        status
                );

            } else {

                log.info(
                        "layer={} method={} durationMs={} status={}",
                        layer,
                        methodName,
                        String.format(
                                java.util.Locale.ROOT,
                                "%.2f",
                                durationMs
                        ),
                        status
                );
            }
        }
    }

    private boolean isRepositoryMethod(
            ProceedingJoinPoint joinPoint) {

        return joinPoint.getTarget()
                instanceof org.springframework.data.repository.Repository;
    }
}