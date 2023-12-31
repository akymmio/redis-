package com.rrdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rrdp.dto.Result;
import com.rrdp.dto.UserDTO;
import com.rrdp.entity.Blog;

import com.rrdp.service.IBlogService;

import com.rrdp.utils.SystemConstants;
import com.rrdp.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

import static com.rrdp.utils.SystemConstants.MAX_PAGE_SIZE;

/**
 * 博客
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量
        //blogService.update().setSql("liked = liked + 1").eq("id", id).update();
        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query().eq("user_id", user.getId()).page(new Page<>(current, MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.success(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        return blogService.queryBlogLikes(id);
    }

    /**
     * 查询用户的blog
     */
    @GetMapping("/of/user")
    public Result queryBlogById(@RequestParam(value = "current", defaultValue = "1") Integer current, @RequestParam("id") Long id) {
/*        LambdaQueryWrapper<Blog> qw=new LambdaQueryWrapper<>();
        qw.eq(Blog::getUserId,id);
        Page<Blog> p = new Page<>(current,MAX_PAGE_SIZE);
        blogService.page(p,qw);
        return Result.success(p.getRecords());*/
        // 根据用户查询
        Page<Blog> page = blogService.query().eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.success(records);
    }

    @GetMapping("of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId")Long max,@RequestParam(value = "offset",defaultValue = "0")Integer offset){
        return blogService.queryBlogOfFollow(max,offset);
    }
}
