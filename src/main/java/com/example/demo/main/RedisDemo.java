package com.example.demo.main;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;

import org.springframework.boot.autoconfigure.cache.CacheProperties.Redis;

public class RedisDemo {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Azure Managed Redis Configuration
    private static final int REDIS_PORT = 10000;
    private static final String REDIS_HOST = "xxxx.koreacentral.redis.azure.net";
    private static final String REDIS_PASSWORD = "<REDIS_PASSWORD>";
    private static final boolean USE_SSL = true; // SSL 사용 여부

    private static final int COUNT = 1000;
    private static final String KEY_PATTERN = "10"; //

    public static void main(String[] args) {
        // Redis 작업 객체 생성
        RedisOperations redisOps = new RedisOperations(REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, USE_SSL);

        try {
            // 1. 데이터 생성 (1000개)
            System.out.println("\n1. Generating " + COUNT + " sample data items:");
            printResult(redisOps.generateData(COUNT));

            // 0. 데이터 삭제 (특정 패턴)
            System.out.println("\n0. Deleting " + KEY_PATTERN + " sample data items:");
            printResult(redisOps.delData(KEY_PATTERN));

            // 2. 모든 키 조회 (KEYS 명령어 사용)
            System.out.println("\n2. Getting all keys using KEYS command:");
            printResult(redisOps.getAllKeys());

            // 3. SCAN을 사용한 키 조회
            System.out.println("\n3. Getting keys using SCAN command:");
            printResult(redisOps.scanKeys("key:*", COUNT));
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            redisOps.close();
        }
    }

    private static void printResult(Map<String, Object> result) {
        try {
            // keys 필드를 제외한 새로운 맵 생성
            Map<String, Object> filteredResult = new HashMap<>(result);
            filteredResult.remove("keys");
            
            // 결과를 pretty print로 출력
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filteredResult);
            System.out.println(json);
        } catch (Exception e) {
            System.err.println("Error printing result: " + e.getMessage());
        }
    }
}
