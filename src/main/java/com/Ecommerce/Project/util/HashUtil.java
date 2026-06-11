package com.Ecommerce.Project.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HashUtil{
    public static String sha256(String input){
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] hashBytes= digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hashValue=new StringBuilder();
            for(byte b:hashBytes){
                String s=Integer.toHexString(0xff & b);
                if(s.length()==1){
                    hashValue.append("0");
                }
                hashValue.append(s);
            }
            return hashValue.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing Failed",e);
        }
    }
}
