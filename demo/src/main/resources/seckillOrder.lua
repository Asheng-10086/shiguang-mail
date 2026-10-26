-- seckill.lua
-- ARGV[1] = productId
-- ARGV[2] = userId
-- ARGV[3] = orderId
-- ARGV[4] = addressId

local productId = ARGV[1]
local userId    = ARGV[2]
local orderId   = ARGV[3]
local addressId = ARGV[4]

-- 可按你的实际 key 规则改这里
local stockKey   = 'seckill:stock:' .. productId
local userSetKey = 'seckill:users:' .. productId
local streamKey  = 'seckill:stream:' 
-- 1. 查库存：不存在或 <= 0 都视为不可秒杀
local stock = redis.call('GET', stockKey)
if (not stock) or tonumber(stock) <= 0 then
    return 3
end

-- 2. 查用户是否已下单
if redis.call('SISMEMBER', userSetKey, userId) == 1 then
    return 4
end

-- 3. 记录用户，防止重复
redis.call('SADD', userSetKey, userId)
redis.call('DECR', stockKey)

-- 4. 投递到 Stream，* 表示由 Redis 自动生成消息 ID
redis.call(
    'XADD', streamKey, '*',
    'productId', productId,
    'userId', userId,
    'orderId', orderId,
    'addressId', addressId
)

return 0