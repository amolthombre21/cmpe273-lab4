package edu.sjsu.cmpe.cache.client;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;


public class DistributedCacheService implements CacheServiceInterface 
{
    private  String cache_serv_url;
    private  String[] serv_url;
    private AtomicInteger success_read_count;
    private AtomicInteger success_write_count;
    int num_server;
    //ctor
    public DistributedCacheService(String serverUrl) 
    {
        this.cache_serv_url = serverUrl;
    }

   //ctor 
    public DistributedCacheService(String...serv_url)
    {
        this.serv_url=serv_url;
        this.num_server = serv_url.length;
    }
    


    @Override
    public String get(long key) 
    {
        HttpResponse<JsonNode> resp = null;	
        try {
            resp = Unirest.get(this.cache_serv_url + "/cache/{key}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key)).asJson();
        } catch (UnirestException e) {
            System.err.println(e);
        }
        String value = resp.getBody().getObject().getString("value");

        return value;
    }


    
    @Override
    public String getAsynch(long key) 
{

        final CountDownLatch counter = new CountDownLatch(num_server);
        HttpResponse<JsonNode> resp = null;
      success_read_count = new AtomicInteger(0);
        final ListMultimap<String,String> successfulServers = ArrayListMultimap.create();

        try {
            for (int i = 0; i < num_server; i++) {

                final String currentServerURL = this.serv_url[i];

                Future<HttpResponse<JsonNode>> future = Unirest.get(currentServerURL + "/cache/{key}")
                        .header("accept", "application/json")
                        .routeParam("key", Long.toString(key)).asJsonAsync(new Callback<JsonNode>() 
			{
                            String currentUrl = currentServerURL;

                            public void failed(UnirestException e) 
			    {
                                System.out.println("request not completed");
                            }

                            public void completed(HttpResponse<JsonNode> resp) 
				{
                                	int code = resp.getCode();
                               	 	Headers headers = resp.getHeaders();
                                	JsonNode body = resp.getBody();
                                	InputStream raw_body = resp.getRawBody();
					System.out.println(resp.getBody());
                                	String value;
                                	if (resp.getBody()!=null)
                                    	value = resp.getBody().getObject().getString("value");
                                    	else
                                        value="fault";
                                    	success_read_count.incrementAndGet();
                                    	successfulServers.put(value, currentUrl);
                                    	counter.countDown();
                                    


                            	}

                            public void cancelled() 
				{
                                	System.out.println("The request has been cancelled");
                            	}

                        });


            }
            	counter.await();
        }
        catch (InterruptedException e)
	{
            e.printStackTrace();
        }



        List<String> same_val_servers = new ArrayList<String>();
        String successValue=null;

        for (String value : successfulServers.keySet()) 
	{

            List<String> receivedserv_url = successfulServers.get(value);
            if(receivedserv_url!=null) 
		{
	                if (receivedserv_url.size() > same_val_servers.size()) {
	                    same_val_servers = receivedserv_url;
	                    successValue = value;
                }
        }
}

       
        List<String> faultServers = new ArrayList<String>();
        for(String srvUrl:serv_url)
	{
            int i=0;
            for(;i<same_val_servers.size();i++)
	    {
                if(srvUrl.equalsIgnoreCase(same_val_servers.get(i)))
                    break;
            }
            if(i>=same_val_servers.size())
	    {
                faultServers.add(srvUrl);

            }
        }
		System.out.println("received from servers.....");
        for (String srvr:same_val_servers)
	{
            System.out.println(srvr);
        }
        System.out.println("values repaired on servers.....");
        for (String srvr:faultServers)
	{
            System.out.println(srvr);
            this.cache_serv_url=srvr;
            put(key,successValue);
        }
     
        return successValue;
    }

    
    
    @Override
    public void put(long key, String value) 
	{
	        HttpResponse<JsonNode> resp = null;
	        try {
	            resp = Unirest
                    .put(this.cache_serv_url + "/cache/{key}/{value}")
                    .header("accept", "application/json")
                    .routeParam("key", Long.toString(key))
                    .routeParam("value", value).asJson();
	        } 
	catch (UnirestException e) 
	{
            System.err.println(e);
        }

        if (resp.getCode() != 200) {
            System.out.println("Loading to caching failed.");
        }
    }
    @Override
    public void putAsynch(long key, String value) 
	{

        try {
            final List<String> successfulServers = new ArrayList<String>();
            success_write_count = new AtomicInteger(0);
            final CountDownLatch counter = new CountDownLatch(num_server);
            for (int i = 0; i < num_server; i++) {
                final String currentServerURL = this.serv_url[i];
                Future<HttpResponse<JsonNode>> future = Unirest
                        .put(currentServerURL + "/cache/{key}/{value}")
                        .header("accept", "application/json")
                        .routeParam("key", Long.toString(key))
                        .routeParam("value", value).asJsonAsync(new Callback<JsonNode>() {
                            String currentUrl = currentServerURL;
                            public void failed(UnirestException e) {
                                System.out.println("request has failed");
                                counter.countDown();
                            }

                            public void completed(HttpResponse<JsonNode> resp) 
			   {
                                int code = resp.getCode();
                                Headers headers = resp.getHeaders();
                                JsonNode body = resp.getBody();
                                InputStream raw_body = resp.getRawBody();
                                success_write_count.incrementAndGet();
                                successfulServers.add(currentUrl);
                                counter.countDown();
                            }

                            public void cancelled() 
			    {
                                System.out.println("request has been cancelled");
                            }

                        });

            }
            counter.await();

            if(num_server%2==0)
		{
                if(success_write_count.intValue()>=(num_server/2))
		{
                    System.out.println("Successful Put on servers...");
                    for(String successfulServer:successfulServers)
			{
                        	System.out.println(successfulServer);
                    	}
                }
                else
		{
                    	System.out.println("Deleting values from Server(s)...");
                    	for (int i = 0; i < successfulServers.size(); i++) 
			{
                        	System.out.println(successfulServers.get(i));
                        	HttpRequestWithBody resp = Unirest.delete(successfulServers.get(i)+"/cache/{key}");
                        	System.out.println(resp);
                    	}
                }


            }
            else
		{
	                if(success_write_count.intValue()>=((num_server/2)+1)){
	                    System.out.println("Successful Put on servers...");
	                    for(String successfulServer:successfulServers)
				{
		                        System.out.println(successfulServer);
		                    }
                }
            else
		{
                    System.out.println("Deleting values from Server(s)...");
                    for (int i = 0; i < successfulServers.size(); i++) 
			{
	                        System.out.println(successfulServers.get(i));
	                        HttpRequestWithBody resp = Unirest.delete(successfulServers.get(i)+"/cache/{key}");
	                        System.out.println(resp);
	                    }
                }


            }
        }
        catch(InterruptedException e)
	{
            e.printStackTrace();
        }

    }



}
