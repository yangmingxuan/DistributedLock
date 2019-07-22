package com.namescome.distributedlock.redis;

import java.util.Collections;

import redis.clients.jedis.Jedis;

import com.namescome.distributedlock.common.DistributedLock;

public class RedisLock implements DistributedLock {

    private final static String SET_SUCCESS = "OK";
    private final static String SET_NO_EXIST = "NX";
    private final static String SET_WITH_EXPIRED_TIME = "PX";    //Counting unit is millisecond
    private final static Long RELEASE_SUCCESS = 1L;
 
    private Jedis jedis;
    private long lockDuration = 30000;   //The lock's expired time
    private long eachWait = 50;      //Each milliseconds for waiting lock
    private long maxWait = 30000;       //The max milliseconds for waiting lock
    private long startTime;    //Start time for waiting
 
    
    
    /**
     * @return the jedis
     */
    public Jedis getConnection() {
        return jedis;
    }



    /**
     * @param jedis the jedis to set
     */
    public void setConnection(Object connection) {
        this.jedis = (Jedis)connection;
    }



    /**
     * @return the lockDuration
     */
    public long getLockDuration() {
        return lockDuration;
    }



    /**
     * @param lockDuration the lockDuration to set
     */
    public void setLockDuration(long lockDuration) {
        this.lockDuration = lockDuration;
    }



    /**
     * @return the eachWait
     */
    public long getEachWait() {
        return eachWait;
    }



    /**
     * @param eachWait the eachWait to set
     */
    public void setEachWait(long eachWait) {
        this.eachWait = eachWait;
    }



    /**
     * @return the maxWait
     */
    public long getMaxWait() {
        return maxWait;
    }



    /**
     * @param maxWait the maxWait to set
     */
    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }



    public RedisLock() {
        
    }


    public boolean tryLock(String LockKey, String LockValue) {
        startTime = System.currentTimeMillis();
        boolean isGetLock = false;
        do {
            isGetLock = getLock(LockKey, LockValue);
            if(!isGetLock) {
                waitLock (LockKey);
            }
        } while(!isGetLock && startTime + maxWait > System.currentTimeMillis());
        return isGetLock;
    }

    private boolean getLock(String LockKey, String LockValue) {
        String result = jedis.set(LockKey, LockValue, SET_NO_EXIST, SET_WITH_EXPIRED_TIME, lockDuration);
        if(SET_SUCCESS.equalsIgnoreCase(result)) {
            return true;
        }
        return false;
    }

    private void waitLock (String LockKey) {
        try {
            Thread.sleep(eachWait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /***
     * To avoid unlocking someone else, the lockValue must be inputed
     * @param LockKey
     * @param LockValue
     * @return
     */
    public boolean unLock(String LockKey, String LockValue) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, Collections.singletonList(LockKey), Collections.singletonList(LockValue));

        if (RELEASE_SUCCESS.equals(result)) {
            return true;
        }
        return false;
    }

}
