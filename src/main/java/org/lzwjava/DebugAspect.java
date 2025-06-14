package org.lzwjava;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public final class DebugAspect {
    private static final Logger log = LoggerFactory.getLogger(DebugAspect.class);

    @After("execution(* org.lzwjava.HelloController.*(..))")
    public void logAfter(JoinPoint joinPoint) {
        log.info("Method executed: {}", joinPoint.getSignature());
    }
}
