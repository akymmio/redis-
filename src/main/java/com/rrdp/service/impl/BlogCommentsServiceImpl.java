package com.rrdp.service.impl;

import com.rrdp.entity.BlogComments;
import com.rrdp.mapper.BlogCommentsMapper;
import com.rrdp.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
