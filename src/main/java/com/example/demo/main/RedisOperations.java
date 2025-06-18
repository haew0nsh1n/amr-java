package com.example.demo.main;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class RedisOperations {
    private final JedisPool jedisPool;
    private final boolean useSsl;

    public RedisOperations(String host, int port, String password, boolean useSsl) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(60));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);

        this.useSsl = useSsl;
        if (password != null && !password.isEmpty()) {
            this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password, useSsl);
        } else {
            this.jedisPool = new JedisPool(poolConfig, host, port);
        }
    }

    public Map<String, Object> generateData(int count) {
        Instant start = Instant.now();
        Map<String, Object> result = new HashMap<>();

        try (Jedis jedis = jedisPool.getResource()) {
            // 기존 데이터 모두 삭제
            Set<String> existingKeys = jedis.keys("*");  // 모든 키를 가져옴
            if (!existingKeys.isEmpty()) {
                jedis.del(existingKeys.toArray(new String[0]));
            }

            // 새로운 데이터 생성
            for (int i = 0; i < count; i++) {
                String key = String.format("key:%d", i);
                String value = String.format("value:%d", i);
                jedis.set(key, value);
            }
            result.put("status", "success");
            result.put("count", count);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }

        Instant end = Instant.now();
        result.put("executionTime", Duration.between(start, end).toMillis());
        return result;
    }

    public Map<String, Object> getAllKeys() {
        Instant start = Instant.now();
        Map<String, Object> result = new HashMap<>();
        List<String> keys = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> redisKeys = jedis.keys("*");
            keys.addAll(redisKeys);
            result.put("status", "success");
            result.put("keys", keys);
            result.put("count", keys.size());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }

        Instant end = Instant.now();
        result.put("executionTime", Duration.between(start, end).toMillis());
        return result;
    }

    public Map<String, Object> scanKeys(String pattern, int count) {
        Instant start = Instant.now();
        Map<String, Object> result = new HashMap<>();
        List<String> keys = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {
            String cursor = "0";
            ScanParams scanParams = new ScanParams().match(pattern).count(count);

            do {
                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                keys.addAll(scanResult.getResult());
                cursor = scanResult.getCursor();
            } while (!"0".equals(cursor));

            result.put("status", "success");
            result.put("keys", keys);
            result.put("count", keys.size());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }

        Instant end = Instant.now();
        result.put("executionTime", Duration.between(start, end).toMillis());
        return result;
    }

    public Map<String, Object> getValue(String key) {
        Instant start = Instant.now();
        Map<String, Object> result = new HashMap<>();

        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key);
            result.put("status", "success");
            result.put("key", key);
            result.put("value", value);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }

        Instant end = Instant.now();
        result.put("executionTime", Duration.between(start, end).toMillis());
        return result;
    }

    public Map<String, Object> delData(String keyPattern) {
        Instant start = Instant.now();
        Map<String, Object> result = new HashMap<>();
        
        try (Jedis jedis = jedisPool.getResource()) {
            // 모든 키를 가져옴
            Set<String> allKeys = jedis.keys("*");
            List<String> keysToDelete = new ArrayList<>();
            
            // keyPattern이 포함된 키만 삭제 대상으로 선정
            for (String key : allKeys) {
                if (key.contains(keyPattern)) {
                    keysToDelete.add(key);
                }
            }
            
            // 삭제할 키가 있는 경우
            result.put("status", "success");
            if (!keysToDelete.isEmpty()) {
                jedis.del(keysToDelete.toArray(new String[0]));
                result.put("deletedCount", keysToDelete.size());
            } else {
                result.put("deletedCount", 0);
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }

        Instant end = Instant.now();
        result.put("executionTime", Duration.between(start, end).toMillis());
        return result;
    }

    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
