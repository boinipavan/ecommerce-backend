package com.Ecommerce.Project.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID="traceId";
    private static final String TRACE_HEADER="X-Trace-Id";
    private static final String USER="user";
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        long start=System.currentTimeMillis();

        String traceId=request.getHeader(TRACE_HEADER);
        if(traceId==null || traceId.isBlank()){
            traceId= UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID,traceId);
        response.setHeader(TRACE_HEADER,traceId);

        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
        if(authentication!=null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())){
            String user=authentication.getName();
            MDC.put(USER,user);
        }

        try{
            filterChain.doFilter(request,response);
        }
        catch (Exception ex){
            log.error("Request Failed {} {}",request.getMethod(),request.getRequestURI(),ex);
            throw ex;
        }
        finally{
            long duration=System.currentTimeMillis()-start;
            log.info("{} {} -> {} ({} ms)",request.getMethod(),request.getRequestURI(),response.getStatus(),duration);
            MDC.remove(TRACE_ID);
            MDC.remove(USER);
        }
    }
}

