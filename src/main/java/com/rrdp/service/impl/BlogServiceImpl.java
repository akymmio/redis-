package com.rrdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rrdp.dto.Result;
import com.rrdp.dto.Scroll;
import com.rrdp.dto.UserDTO;
import com.rrdp.entity.Blog;
import com.rrdp.entity.Follow;
import com.rrdp.entity.User;
import com.rrdp.mapper.BlogMapper;
import com.rrdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rrdp.service.IUserService;
import com.rrdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.rrdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.rrdp.utils.RedisConstants.FOLLOW_BOX_KEY;
import static com.rrdp.utils.SystemConstants.MAX_PAGE_SIZE;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private FollowServiceImpl followService;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current,MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.success(records);
    }
    @Override
    public Result queryBlogById(Long id) {
        //查询blog
        Blog blog = getById(id);
        if(blog==null){
            return Result.fail("笔记不存在");
        }
        //查询blog的用户
        queryBlogUser(blog);
        //查询是否被点赞
        isBlogLiked(blog);
        return Result.success(blog);
    }


    /**
     * 查询博客所属的用户信息
     * @param blog
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user =userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    /**
     * 博客是否被当前登录用户点赞
     */
    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if(user==null) return;
        Long userId =user.getId();
        //判断当前用户是否点赞
        String key="blog:liked:"+blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!=null);
    }

    /**
     * 点赞和取消赞功能
     */
    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        //判断用户是否点赞
        String key=BLOG_LIKED_KEY+id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        //点赞，保存用户到redis中（set）,
        if(score==null){
            boolean update = update().setSql("liked=liked+1").eq("id", id).update();
            if(update){
                //保存到zset中
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }
        //取消赞，删除用户
        else{
            boolean remove= update().setSql("liked=liked-1").eq("id", id).update();
            if(remove){
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.success();
    }

    /**
     * 点赞列表
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id) {
        String key=BLOG_LIKED_KEY+id;
        //查询前五名点赞用户
        Set<String> set = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(set==null || set.isEmpty()) return Result.success(Collections.emptyList());
        //解析id，String->Long
        List<Long> ids = set.stream().map(Long::valueOf).collect(Collectors.toList());
        //根据id查询用户
        List<User> list = userService.query().in("id", ids).last("order by id desc").list();
        //转化，User->UserDTO
        List<UserDTO> userDTOs = list.stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.success(userDTOs);
    }

    /**
     * 保存博客，同时推送给粉丝
     * @param blog
     * @return
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean save = save(blog);
        if(!save) return Result.fail("新增笔记失败");
        //推送blog给粉丝
        List<Follow> followUsers= followService.query().eq("follow_user_id", user.getId()).list();
        for(Follow f:followUsers){
            //获取粉丝的id
            Long userId = f.getUserId();
            //推送到粉丝的收件箱
            String key=FOLLOW_BOX_KEY+userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }
        // 返回id
        return Result.success(blog.getId());
    }


    /**
     *根据时间戳大小来实现分页滚动查询（redis中以时间戳作为score）
     * @param max 上一次查询到的最小值，是这一次查询的最大值
     * @param offset 偏移量
     * @return
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        //查询用户收件箱
        String key=FOLLOW_BOX_KEY+userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if(typedTuples==null || typedTuples.isEmpty()) return Result.success();

        //解析数据，minTime(时间戳)，offset，blogId
        List<Long> ids=new ArrayList<>(typedTuples.size());
        long minTime=0;
        int off=1;
        //获取blog的id，更新偏移量，时间戳
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            //获取blog的id
            ids.add(Long.valueOf(Objects.requireNonNull(typedTuple.getValue())));
            //获取时间戳（score）
            long time = Objects.requireNonNull(typedTuple.getScore()).longValue();
            if(time==minTime){
                off++;
            }
            else{
                off=1;
                minTime=time;
            }
        }
        //根据id查询blog，(要保持有序)
        List<Blog> blogs = query().in("id", ids).last("order by id desc").list();
        //点赞信息
        for (Blog blog : blogs) {
            //查询blog的用户
            queryBlogUser(blog);
            //查询是否被点赞
            isBlogLiked(blog);
        }
        //封装数据
        Scroll scroll=new Scroll();
        scroll.setList(blogs);
        scroll.setOffset(off);
        scroll.setMinTime(minTime);

        return Result.success(scroll);
    }
}
