package com.Ecommerce.Project.infrastructure.hashing;

import com.Ecommerce.Project.util.HashUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class HashService {
    private final ObjectMapper mapper;

    public HashService(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    public String generateHash(Object request){
        try {
            String str = mapper.writeValueAsString(request);
            return HashUtil.sha256(str);
        }
        catch (RuntimeException | JsonProcessingException e){
            throw new RuntimeException("Hash generation failed",e);
        }
    }
}
