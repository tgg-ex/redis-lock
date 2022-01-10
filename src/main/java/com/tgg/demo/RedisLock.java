package com.tgg.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

/**
 * @author zeng
 * <p>
 * redis 同步锁配置
 */
@Component
public class RedisLock {

    @Autowired
    private JedisPool jedisPool;


    /**
     * redis 锁成功标识常量
     */
    private static final Long RELEASE_SUCCESS = 1L;
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "EX";
    private static final String LOCK_SUCCESS = "OK";
    /**
     * 加锁 Lua 表达式。
     */
    private static final String RELEASE_TRY_LOCK_LUA =
            "if redis.call('setNx',KEYS[1],ARGV[1]) == 1 then return redis.call('expire',KEYS[1],ARGV[2]) else return 0 end";
    /**
     * 解锁 Lua 表达式.
     */
    private static final String RELEASE_RELEASE_LOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    /**
     * 加锁
     * 支持重复，线程安全
     * 既然持有锁的线程崩溃，也不会发生死锁，因为锁到期会自动释放
     *
     * @param lockKey    加锁键
     * @param userId     加锁客户端唯一标识（采用用户id, 需要把用户 id 转换为 String 类型）
     * @param expireTime 锁过期时间
     * @return OK 如果key被设置了
     */
    public boolean tryLock(String lockKey, String userId, long expireTime) {
//        Jedis jedis = JedisUtils.getInstance().getJedis();
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.select(0);
            String result = jedis.set(lockKey, userId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            if (LOCK_SUCCESS.equals(result)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null)
                jedis.close();
        }

        return false;
    }

    /**
     * 解锁
     * 与 tryLock 相对应，用作释放锁
     * 解锁必须与加锁是同一人，其他人拿到锁也不可以解锁
     *
     * @param lockKey 加锁键
     * @param userId  解锁客户端唯一标识（采用用户id, 需要把用户 id 转换为 String 类型）
     * @return
     */
    public boolean releaseLock(String lockKey, String userId) {
        Jedis jedis = jedisPool.getResource();
        try {
            jedis.select(0);
            Object result = jedis.eval(RELEASE_RELEASE_LOCK_LUA, Collections.singletonList(lockKey), Collections.singletonList(userId));
            if (RELEASE_SUCCESS.equals(result)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null)
                jedis.close();
        }

        return false;
    }
}
