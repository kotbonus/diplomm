package grpc.demo.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MetricsAspect {

    private static final Logger logger = LoggerFactory.getLogger(MetricsAspect.class);

    @Autowired
    private MeterRegistry meterRegistry;

    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logHttpRequestTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Object result = joinPoint.proceed();
            

            Counter.builder("http.requests.success")
                    .tag("method", methodName)
                    .tag("class", className)
                    .tag("application", "wishlist-app")
                    .register(meterRegistry)
                    .increment();
            
            return result;
        } catch (Exception e) {

            Counter.builder("http.requests.error")
                    .tag("method", methodName)
                    .tag("class", className)
                    .tag("error", e.getClass().getSimpleName())
                    .tag("application", "wishlist-app")
                    .register(meterRegistry)
                    .increment();
            
            throw e;
        } finally {
            sample.stop(Timer.builder("http.request.duration")
                    .tag("method", methodName)
                    .tag("class", className)
                    .tag("application", "wishlist-app")
                    .register(meterRegistry));
        }
    }


    @Around("execution(* grpc.demo.service.*.*(..))")
    public Object logServiceMethodTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Object result = joinPoint.proceed();
            
            Counter.builder("service.methods.success")
                    .tag("method", methodName)
                    .tag("service", className)
                    .tag("application", "wishlist-app")
                    .register(meterRegistry)
                    .increment();
            
            return result;
        } catch (Exception e) {
            Counter.builder("service.methods.error")
                    .tag("method", methodName)
                    .tag("service", className)
                    .tag("error", e.getClass().getSimpleName())
                    .tag("application", "wishlist-app")
                    .register(meterRegistry)
                    .increment();
            
            throw e;
        } finally {
            sample.stop(Timer.builder("service.method.duration")
                    .tag("method", methodName)
                    .tag("service", className)
                    .tag("application", "wishlist-app")
                    .register(meterRegistry));
        }
    }
}
