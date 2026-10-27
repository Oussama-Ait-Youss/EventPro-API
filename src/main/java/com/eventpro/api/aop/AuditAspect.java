package com.eventpro.api.aop;

import com.eventpro.api.aop.annotation.Auditable;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log =
            LoggerFactory.getLogger("AUDIT_LOG");

    @Around("@annotation(auditable)")
    public Object auditOperation(
            ProceedingJoinPoint joinPoint,
            Auditable auditable) throws Throwable {

        long startTime = System.nanoTime();

        String operationId = UUID.randomUUID().toString();

        String methodName =
                joinPoint.getSignature().toShortString();

        String httpMethod = "-";
        String requestPath = "-";

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

        if (attributes != null) {

            HttpServletRequest request =
                    attributes.getRequest();

            httpMethod = request.getMethod();

            requestPath = request.getRequestURI();
        }

        try {

            Object result = joinPoint.proceed();

            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startTime
            );

            log.info(
                    "operationId={} action={} resource={} method={} httpMethod={} path={} status=SUCCESS durationMs={}",
                    operationId,
                    auditable.action(),
                    auditable.resource(),
                    methodName,
                    httpMethod,
                    requestPath,
                    durationMs
            );

            return result;

        } catch (Throwable exception) {

            long durationMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - startTime
            );

            log.warn(
                    "operationId={} action={} resource={} method={} httpMethod={} path={} status=FAILED durationMs={} exception={}",
                    operationId,
                    auditable.action(),
                    auditable.resource(),
                    methodName,
                    httpMethod,
                    requestPath,
                    durationMs,
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }
}