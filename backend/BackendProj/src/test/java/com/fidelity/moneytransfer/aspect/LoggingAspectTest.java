package com.fidelity.moneytransfer.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock private ProceedingJoinPoint joinPoint;
    @Mock private Signature signature;

    private final LoggingAspect aspect = new LoggingAspect();

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("transfer");
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1"});
    }

    @Test
    void logServiceMethods_success_returnsResult() throws Throwable {
        when(joinPoint.proceed()).thenReturn("result");
        Object result = aspect.logServiceMethods(joinPoint);
        assertEquals("result", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logServiceMethods_exception_logsAndRethrows() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aspect.logServiceMethods(joinPoint));
        assertEquals("boom", ex.getMessage());
    }
}
