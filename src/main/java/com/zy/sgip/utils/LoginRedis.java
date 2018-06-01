package com.zy.sgip.utils;

import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Component
public class LoginRedis {

    /*@Value("${redis.host}")
    private String host;
    @Value("${redis.port}")
    private String port;
    @Value("${redis.timeout}")
    private String timeout;
    @Value("${redis.pwd}")
    private String password;
    @Value("${redis.db}")
    private String db;
    @PostConstruct
    public void init() {
        System.out.println("++++++++++++++############### " + host);
    }*/

    public static Jedis login() {
        JedisPool jedisPool = null;
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(10);
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxWaitMillis(10000);
        poolConfig.setTestOnBorrow(true);
        //jedisPool = new JedisPool(poolConfig, host, Integer.valueOf(port), Integer.valueOf(timeout), password, Integer.valueOf(db));
        jedisPool = new JedisPool(poolConfig, "192.168.10.9", 6379, 3000, "test", 4);
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }
}
