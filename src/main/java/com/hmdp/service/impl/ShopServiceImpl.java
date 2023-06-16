package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  Service implementation class
 * </p>
 *
 * @author Josephinesss
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // cache penetration
//         Shop shop = cacheClient
//                 .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

//         hotspot invalid using mutex
//         Shop shop = queryWithMutex(id);
//
//         hotspot invalid using logic expire
        Shop shop = cacheClient.queryWithLogicalExpire(
                CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if(shop == null){
            return Result.fail("Shop does not exist");
        }

        // return
        return Result.ok(shop);
    }

    // set thread pool
  /*  private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    // use logical expire to solve hotspot invalid
    public Shop queryWithLogicalExpire(Long id){
        // search shop cache from Redis
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        // determine if it exists
        if(StrUtil.isBlank(shopJson)){
            // cache miss, return null
            return null;
        }

        // hit cache, deserialize json into object
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        // determine if it expires
        if(expireTime.isAfter(LocalDateTime.now())){
            // not expire, return shop info
            return shop;
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
                    this.saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // release lock
                    unlock(lockKey);
                }
            });
        }

        // return shop info
        return shop;
    }*/

    // use mutex to solve hotspot invalid
    public Shop queryWithMutex(Long id){
        // search shop cache from Redis
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        // determine if it exists
        if(StrUtil.isNotBlank(shopJson)){
            // exists, return java object (convert json to java class)
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // determine if it is a null value
        if(shopJson != null){
            return null;
        }

        // rebuild cache
        // get mutex
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);

            // determine if it gets
            if(isLock){
                // fail, sleep and retry
                Thread.sleep(50);
                return queryWithMutex(id);
            }


            // success, search database by id
            shop = getById(id);

            // not exist, return exception
            if(shop == null){
                // write null into redis
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,
                        "",
                        CACHE_NULL_TTL,
                        TimeUnit.MINUTES);

                return null;
            }

            // exist, write shop data to Redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,
                    JSONUtil.toJsonStr(shop),
                    CACHE_SHOP_TTL,
                    TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // release mutex
            unlock(lockKey);
        }

        // return
        return shop;
    }

    // cache penetration
    /* public Shop queryWithPassThrough(Long id){
        // search shop cache from Redis
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);

        // determine if it exists
        if(StrUtil.isNotBlank(shopJson)){
            // exists, return java object (convert json to java class)
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // determine if it is a null value
        if(shopJson != null){
            return null;
        }

        // not exist, search database by id
        Shop shop = getById(id);

        // not exist, return exception
        if(shop == null){
            // write null into redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,
                    "",
                    CACHE_NULL_TTL,
                    TimeUnit.MINUTES);

            return null;
        }

        // exist, write shop data to Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,
                JSONUtil.toJsonStr(shop),
                CACHE_SHOP_TTL,
                TimeUnit.MINUTES);

        // return
        return shop;
    }

     */

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    public void saveShop2Redis(Long id, Long expireSeconds){
        // search shop data
        Shop shop = getById(id);

        // package logic expiration time
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        // write to redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("Shop ID cannot be null");
        }

        // update database
        updateById(shop);
        // delete cache
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // determine if need to query by location
        if(x == null || y == null){
            // no need to query by location; query by db
            Page<Shop> page = query().
                    eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        // calculate page parameter
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // query redis and sort and page
        // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );

        // parse id
        if(results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        if(content.size() <= from){
            // no next page
            return Result.ok(Collections.emptyList());
        }
        // from - end
        List<Long> ids = new ArrayList<>(content.size());
        Map<String, Distance> distanceMap = new HashMap<>(content.size());
        content.stream().skip(from).forEach(result -> {
            // get shop id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // get distance
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });

        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        return Result.ok(shops);
    }
}
