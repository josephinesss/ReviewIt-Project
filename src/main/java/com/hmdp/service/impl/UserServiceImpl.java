package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * Service implementation
 * </p>
 *
 * Implementation of the {@link IUserService} interface that provides user-related services.
 * This class extends {@link ServiceImpl} and uses {@link UserMapper} to interact with the database.
 *
 * @author Josephinesss
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * Use Redis template to store and retrieve data from Redis
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Sends a verification code to the specified phone number and saves it to Redis.
     *
     * @param phone   the phone number to send the verification code to
     * @param session the HttpSession object
     * @return a {@link Result} object indicating whether the operation was successful or not
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // check phone number is valid or not
        if(RegexUtils.isPhoneInvalid(phone)){
            // if it is not valid, return exception
            return Result.fail("Invalid Phone Number");
        }

        // if it is valid, generate code
        String code = RandomUtil.randomNumbers(6);

        // save code to Redis
        // data type: String; use Hash to save; and set expiration time for code
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // send code (simulation)
        log.debug("Successfully send the code, code:{}", code);

        // return ok
        return Result.ok();
    }

    /**
     * User login using the phone number and verification code.
     *
     * @param loginForm the login form submitted by the user
     * @param session   the HttpSession object
     * @return a {@link Result} object containing the login token if the operation was successful
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // check phone number
        // it is likely that user would enter a wrong phone number at this time, so check it twice
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            // if it is not valid, return exception
            return Result.fail("Invalid Phone Number");
        }

        // get and check verification code from Redis
        Object cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.equals(code)){
            // If the code mismatches, return exception
            return Result.fail("Incorrect code");
        }

        // If the code matches, find user by phone number
        // use MyBatis
        User user = query().eq("phone", phone).one();

        // determine if the user exists
        if(user == null){
            // if the user does not exist, create new user and save it tpo database
            user = createUserWithPhone(phone);
        }

        // save user info to redis
        // generate token to login
        String token = UUID.randomUUID().toString(true);

        // convert user object to HashMap
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                .setIgnoreNullValue(true)
                .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        // save
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+ token, userMap);

        // set token expiration time
        stringRedisTemplate.expire(LOGIN_USER_KEY+ token, LOGIN_USER_TTL, TimeUnit.SECONDS);


        // return token
        return Result.ok(token);
    }


    /**
     * Records user sign-in
     *
     * @return a {@link Result} object indicating whether the operation was successful or not
     */
    @Override
    public Result sign() {
        // get current user
        Long userId = UserHolder.getUser().getId();

        // get date
        LocalDateTime now = LocalDateTime.now();

        // key
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        // get day of current date
        int dayOfMonth = now.getDayOfMonth();

        // write into redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth-1, true);

        return Result.ok();
    }

    /**
     * Counts the number of days the user has signed in this month.
     *
     * @return a {@link Result} object with the count of consecutive days the user has signed in this month.
     */
    @Override
    public Result signCount() {
        // get current user
        Long userId = UserHolder.getUser().getId();

        // get date
        LocalDateTime now = LocalDateTime.now();

        // key
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        // get day of current date
        int dayOfMonth = now.getDayOfMonth();

        // get all sign log until today of this month, return decimal numbers
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if(result == null || result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num == null || num == 0){
            return Result.ok(0);
        }

        // loop traversal
        int count = 0;
        while(true){
            // let num and 1 do & operation and get the last bit
            if((num & 1)  == 0){
                // if 0, finish
                break;
            } else {
                // if not 0, count + 1
                count++;
            }
            // right move the numbers
            num >>>= 1;
        }
        return Result.ok(count);
    }

    /**
     * Creates a new user with the specified phone number and saves it to the database.
     *
     * @param phone the phone number of the new user
     * @return the newly created {@link User} object
     */
    private User createUserWithPhone(String phone){
        // create new user
        User newUser = new User();
        newUser.setPhone(phone);
        newUser.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));

        // save new user
        save(newUser);
        return newUser;
    }
}
