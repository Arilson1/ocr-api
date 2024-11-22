package com.arilson.ocr.services;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class JedisService {

    private final JedisPool jedisPool;

    public JedisService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public String find(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(key);
        }
    }

    public void save(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, 300, value);
        }
    }

    public void delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }

}
