package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 关注
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService iFollowService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id")Long id,@PathVariable("isFollow")Boolean isFollow){
        return iFollowService.follow(id,isFollow);
    }
    @GetMapping("/or/not/{id}")
    public Result follow(@PathVariable("id")Long id){
        return iFollowService.isFollow(id);
    }

    @GetMapping("/common/{id}")
    public Result followCommon(@PathVariable("id")Long id){
        return iFollowService.followCommon(id);
    }

}
