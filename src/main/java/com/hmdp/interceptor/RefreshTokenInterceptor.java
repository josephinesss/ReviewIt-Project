package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements AsyncHandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // get token from request header
        String token = request.getHeader("authorization");

        // determine if token is empty
        if(StrUtil.isBlank(token)){
            return true;
        }

        // get user from Redis based on token
        Map<Object, Object> userMap = stringRedisTemplate
                .opsForHash()
                .entries(RedisConstants.LOGIN_USER_KEY + token);

        // determine if the user exists
        if(userMap.isEmpty()) {
            return true;
        }

        // convert Hash data to userDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        // if the user exists, save user to ThreadLocal
        UserHolder.saveUser((UserDTO) userDTO);

        // refresh token expiration time
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,
                RedisConstants.LOGIN_USER_TTL,
                TimeUnit.SECONDS);

        // release
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // remove user
        UserHolder.removeUser();
    }
}
