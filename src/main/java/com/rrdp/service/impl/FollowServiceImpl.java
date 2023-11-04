package com.rrdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rrdp.dto.Result;
import com.rrdp.dto.UserDTO;
import com.rrdp.entity.Follow;
import com.rrdp.mapper.FollowMapper;
import com.rrdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rrdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserServiceImpl userService;

    /**
     * 关注和取关功能
     *
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            return Result.fail("用户未登录");
        }
        Long userId = userDTO.getId();
        String key = "fellows:" + userId;
        //判断关注还是取关
        if (isFollow) {
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean isSuccess = save(follow);
            //存入redis
            if (isSuccess) {
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        }
        //取关
        else {
            LambdaQueryWrapper<Follow> qw = new LambdaQueryWrapper<>();
            qw.eq(Follow::getFollowUserId, followUserId).eq(Follow::getUserId, userId);
            boolean isSuccess = remove(qw);
            if (isSuccess) {
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.success();
    }

    /**
     * 查询是否关注
     *
     * @param
     * @return
     */
    @Override
    public Result isFollow(Long followUserId) {
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            return Result.fail("用户未登录");
        }
        Long userId = userDTO.getId();
        //查询是否关注
        Long count = query().eq("follow_user_id", followUserId).eq("user_id", userId).count();
        return Result.success(count > 0);
    }

    /**
     * 查询共同关注
     * @param id 用户的id userid 登录账号的id
     * @return
     */

    @Override
    public Result followCommon(Long id) {
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null) {
            return Result.fail("用户未登录");
        }
        Long userId = userDTO.getId();
        String key1 = "fellows:" + userId;
        String key2 = "fellows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect == null || intersect.isEmpty()) return Result.success(Collections.emptyList());
        //解析id
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //查询用户
        List<UserDTO> users = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.success(users);
    }
}
