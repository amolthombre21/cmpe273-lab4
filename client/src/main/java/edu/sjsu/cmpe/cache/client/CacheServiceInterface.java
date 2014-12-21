package edu.sjsu.cmpe.cache.client;

/**
 * Cache Service Interface
 * 
 */
public interface CacheServiceInterface {
    public String get(long key);
    public String getAsynch(long key);
    public void put(long key, String value);
    public void putAsynch(long key, String value);
}
