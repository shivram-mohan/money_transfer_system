package com.fidelity.moneytransfer.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoggingAspectTest {

    private LoggingAspect aspect;
    private ProceedingJoinPoint joinPoint;

    @BeforeEach
    void setUp() {
        aspect = new LoggingAspect();
        joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.getName()).thenReturn("doThing");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", 42});
    }

    @Test
    void logsAndReturnsResultOnSuccess() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.logServiceMethods(joinPoint);

        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logsAndRethrowsOnException() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class,
                () -> aspect.logServiceMethods(joinPoint));
    }
}
