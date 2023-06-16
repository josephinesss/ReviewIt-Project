package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  Service implementation
 * </p>
 *
 * @author Josephinesss
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // get login user
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;

        // determine follow or unfollow
        if(isFollow){
            // follow, put new data
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean save = save(follow);
            if(save){
                // put following user id to redis set; sadd userId followerUserId
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }

        } else {
            // unfollow, delete; delete from to_follow where userId = ? and follow_user_id = ?
            boolean remove = remove(new QueryWrapper<Follow>()
                    .eq("follow_user_id", followUserId)
                    .eq("user_id", userId));

            if(remove){
                // delete following user id from redis
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }

        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        // get user
        Long userId = UserHolder.getUser().getId();

        // query follow or not; select * from to_follow where user_id = ? and follow_user_id = ?
        Integer count = query().eq("follow_user_id", followUserId).eq("user_id", userId).count();

        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        // get current user
        Long userId = UserHolder.getUser().getId();
        String key1 = "follows:" + userId;
        String key2 = "follows:" + id;
        // search intersect
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if(intersect == null){
            // no intersect
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(users);
    }
}
