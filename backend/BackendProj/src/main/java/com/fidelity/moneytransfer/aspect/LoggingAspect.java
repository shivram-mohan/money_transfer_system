

package com.fidelity.moneytransfer.aspect;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    public LoggingAspect() {
    }

    @Around("execution(* com.fidelity.moneytransfer.service.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        log.info("==> Entering {}.{}() with arguments: {}", new Object[]{className, methodName, Arrays.toString(args)});
        long startTime = System.currentTimeMillis();

        long executionTime;
        try {
            Object result = joinPoint.proceed();
            executionTime = System.currentTimeMillis() - startTime;
            log.info("<== Exiting {}.{}() with result: {} | Execution time: {}ms", new Object[]{className, methodName, result, executionTime});
            return result;
        } catch (Exception var10) {
            executionTime = System.currentTimeMillis() - startTime;
            log.error("<== Exception in {}.{}() | Execution time: {}ms | Error: {}", new Object[]{className, methodName, executionTime, var10.getMessage()});
            throw var10;
        }
    }
}
