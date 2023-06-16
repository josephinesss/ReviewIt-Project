package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExLongExpire(String key, Object value, Long time, TimeUnit unit){
        // set logic expiration time
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        // write into redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    // cache penetration
    public <T, ID> T queryWithPassThrough(
            String keyPrefix, ID id, Class<T> type, Function<ID, T> dbFallback, Long time, TimeUnit unit){
        // search shop cache from Redis
        String json = stringRedisTemplate.opsForValue().get(keyPrefix + id);

        // determine if it exists
        if(StrUtil.isNotBlank(json)){
            // exists, return java object (convert json to java class)
            return JSONUtil.toBean(json, type);
        }

        // determine if it is a null value
        if(json != null){
            return null;
        }

        // not exist, search database by id
        T t = dbFallback.apply(id);

        // not exist, return exception
        if(t == null){
            // write null into redis
            stringRedisTemplate.opsForValue().set(keyPrefix+ id,
                    "",
                    CACHE_NULL_TTL,
                    TimeUnit.MINUTES);

            return null;
        }

        // exist, write shop data to Redis
        this.set(keyPrefix + id, t, time, unit);

        // return
        return t;
    }

    // set thread pool
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    // use logical expire to solve hotspot invalid
    public <T, ID> T queryWithLogicalExpire(
            String keyPrefix, ID id, Class<T> type, Function<ID,T> dbFallback, Long time, TimeUnit unit){
        // search shop cache from Redis
        String json = stringRedisTemplate.opsForValue().get(keyPrefix + id);

        // determine if it exists
        if(StrUtil.isBlank(json)){
            // cache miss, return null
            return null;
        }

        // hit cache, deserialize json into object
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        T t = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();

        // determine if it expires
        if(expireTime.isAfter(LocalDateTime.now())){
            // not expire, return shop info
            return t;
        }

        // expires, reconstruct cache
        // get mutex
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);

        // determine if it successfully get mutex
        if(isLock){
            // successfully get mutex, open an independent thread to realize cache reconstruction
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // rebuild cache
                    // search db
                    T t1 = dbFallback.apply(id);
                    // write into redis
                   this.setWithLogicalExLongExpire(keyPrefix + id, t1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // release lock
                    unlock(lockKey);
                }
            });
        }

        // return shop info
        return t;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

}
