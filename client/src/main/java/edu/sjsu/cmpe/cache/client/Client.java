package edu.sjsu.cmpe.cache.client;

public class Client 
{

    	public static void main(String[] args) throws Exception 
	{
        	System.out.println("Starting Cache Client...");
        
		
		CacheServiceInterface cache = new DistributedCacheService(
                "http://localhost:3000");
        
		CacheServiceInterface myCache = new DistributedCacheService(
                "http://localhost:3000","http://localhost:3001","http://localhost:3002");


        	System.out.println("inserting (1 -> a)");
        	myCache.putAsynch(1, "a");
        	System.out.println("sleeping  30 seconds...");
        	Thread.sleep(30000);
        	System.out.println("updating inserting (1 -> b)");
        	myCache.putAsynch(1, "b");
        	System.out.println("sleeping  30 seconds...");
        	Thread.sleep(30000);
        	System.out.println("retrieving values...");
        	String value = myCache.getAsynch(1);
        	System.out.println("Value received: "+value);
        	System.out.println("exiting Cache Client...");
    	}

}
