package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    /**
     * 关注和取关功能
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        UserDTO userDTO = UserHolder.getUser();
        if(userDTO==null){
            return Result.fail("用户未登录");
        }
        Long userId = userDTO.getId();
        //判断关注还是取关
        if(isFollow){
            Follow follow=new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            save(follow);
        }
        //取关
        else{
            LambdaQueryWrapper<Follow> qw=new LambdaQueryWrapper<>();
            qw.eq(Follow::getFollowUserId,followUserId).eq(Follow::getUserId,userId);
            remove(qw);
        }
        return Result.success();
    }

    /**
     * 查询是否关注
     * @param
     * @return
     */
    @Override
    public Result isFollow(Long followUserId) {
        UserDTO userDTO = UserHolder.getUser();
        if(userDTO==null){
            return Result.fail("用户未登录");
        }
        Long userId = userDTO.getId();
        //查询是否关注
        Long count = query().eq("follow_user_id", followUserId).eq("user_id", userId).count();
        return Result.success(count>0);
    }
}
