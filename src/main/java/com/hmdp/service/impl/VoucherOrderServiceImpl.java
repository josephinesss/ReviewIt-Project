package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 *  Service implementation
 * </p>
 */

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static{
        SECKILL_SCRIPT= new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{
        String queueName = "stream.orders";

        @Override
        public void run() {
            while(true){
                try {
                    // get order from message queue XREADGROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS streams.orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );

                    // determine if successfully get the msg
                    if(list == null || list.isEmpty()){
                        // if failed, there is no msg, continue to next cycle
                        continue;
                    }

                    // Parsing the order information in the message
                    MapRecord<String, Object, Object> record= list.get(0);
                    Map<Object, Object> values = record.getValue();
                    // change values to order object
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);

                    // create order
                    handleVoucherOrder(voucherOrder);

                    // ACK confirm SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());

                } catch (Exception e) {
                    log.error("Error in handling order", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while(true){
                try {
                    // get order from pending list XREADGROUP g1 c1 COUNT 1 STREAMS streams.orders 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );

                    // determine if successfully get the msg
                    if(list == null || list.isEmpty()){
                        // if failed, there is no msg in pending list, end cycle
                        break;
                    }

                    // Parsing the order information in the message
                    MapRecord<String, Object, Object> record= list.get(0);
                    Map<Object, Object> values = record.getValue();
                    // change values to order object
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);

                    // create order
                    handleVoucherOrder(voucherOrder);

                    // ACK confirm SACK stream.orders g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());

                } catch (Exception e) {
                    log.error("Error in handling pending list", e);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /*
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
    private class VoucherOrderHandler implements Runnable{

        @Override
        public void run() {
            while(true){
                try {
                    // get order from queue
                    VoucherOrder voucherOrder = orderTasks.take();
                    // create order
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    log.error("Error in handling order", e);
                }
            }
        }
    }
     */

    private void handleVoucherOrder(VoucherOrder voucherOrder){
        // get user
        Long userId = voucherOrder.getUserId();

        // create lock object
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        // get lock
        boolean isLock = lock.tryLock();

        // determine if lock has been successfully got
        if(!isLock){
            // failure
            log.error("One person can only purchase once");
            return;
        }
        // get proxy object
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            // release lock
            lock.unlock();
        }
    }

    private IVoucherOrderService proxy;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // get user
        Long userId = UserHolder.getUser().getId();

        // get order id
        long orderId = redisIdWorker.nextId("order");

        // execute lua script
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );

        // determine if the result = 0
        int r = result.intValue();
        if(r != 0){
            // not equals 0, representing that the user cannot purchase
            return Result.fail(r == 1 ? "insufficient stock" : "You can not purchase more than once");
        }

        // get proxy
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        // return order id
        return Result.ok(orderId);
    }

    /*
    @Override
    public Result seckillVoucher(Long voucherId) {
        // get user
        Long userId = UserHolder.getUser().getId();

        // execute lua script
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );

        // determine if the result = 0
        int r = result.intValue();
        if(r != 0){
            // not equals 0, representing that the user cannot purchase
            return Result.fail(r == 1 ? "insufficient stock" : "You can not purchase more than once");
        }

        // create order
        long orderId = redisIdWorker.nextId("order");
        VoucherOrder voucherOrder = new VoucherOrder();
        // voucher id
        voucherOrder.setId(orderId);
        // user id
        voucherOrder.setUserId(userId);
        // coupon id
        voucherOrder.setVoucherId(voucherId);
        // save to blocking queue
        orderTasks.add(voucherOrder);

        // get proxy
        proxy = (IVoucherOrderService) AopContext.currentProxy();

        // return order id
        return Result.ok(orderId);
    }
    */


    /*
    @Override
    public Result seckillVoucher(Long voucherId) {

        // search voucher
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        // determine if the voucher time starts
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            // has not started yet
            return Result.fail("The coupon campaign has not started yet.");
        }
        //  determine if the voucher time ends
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            // has ended
            return Result.fail("The coupon campaign has ended.");
        }

        // determine the adequacy of the stock
        if(voucher.getStock() < 1){
            return Result.fail("Not enough stock!");
        }

        // need synchronized first
        Long userId = UserHolder.getUser().getId();
//        synchronized (userId.toString().intern()) {
        // create lock objects
//        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        // get lock
        boolean isLock = lock.tryLock();

        // determine if lock has been successfully got
        if(!isLock){
            // failure
            return Result.fail("One person can only purchase once");
        }
        // get proxy object
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // release lock
            lock.unlock();
        }
//        }
    }
     */

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // per person per order
        Long userId = voucherOrder.getUserId();

        // search order
        Integer count = query().eq("voucher_id", voucherOrder.getVoucherId()).eq("user_id", userId).count();
        // determine if it exists
        if (count > 0) {
            // user has already purchased
            log.error("User has already purchased.");
            return;
        }

        // deduct stock with MyBatis
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherOrder.getVoucherId()) // where id = ?
                .gt("stock", 0) // and stock > 0
                .update();

        if (!success) {
            // fail to dedeuct stock -- because of stock number
            log.error("Not enough stock!");
            return;
        }

        // writer order into database
        save(voucherOrder);
    }

}

