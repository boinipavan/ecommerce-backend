package com.Ecommerce.Project.record;

import com.Ecommerce.Project.enums.ErrorCode;

import java.time.LocalDateTime;

public record ErrorResponse(LocalDateTime timestamp, int status, ErrorCode error, String code, String message, String traceId){}
