package com.Ecommerce.Project.scheduler;

import com.Ecommerce.Project.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupScheduler {
    private final IdempotencyService idempotencyService;

    @Scheduled(cron = "0 0 * * * *")
    public void cleanUp(){
        MDC.put("traceId","SYSTEM");
        MDC.put("user","SYSTEM");
        try {
            idempotencyService.cleanUpExpiredIdempotencyRecords();
        }
        finally{
            MDC.remove("traceId");
            MDC.remove("user");
        }
    }
}
