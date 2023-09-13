--优惠卷id
local voucherId=ARGV[1]
--用户id
local userId=ARGV[2]

--库存key
local stockKey='seckill:stock:' .. voucherId
--订单key
local orderKey='seckill:order:' .. voucherId

if(tonumber(redis.call('get',stockKey))<=0) then
    --库存不足，返回1
    return 1
end

if(redis.call('sismember',orderKey,userId)==1) then
    --存在，重复下单,返回2
    return 2
end

--扣减库存，下单
redis.call('incrby',stockKey,-1)
redis.call('sadd',orderKey,userId)

return 0