package com.cockroachlabs.concurrency;


import com.beust.jcommander.JCommander;
import com.cockroachlabs.Application;
import com.cockroachlabs.services.CustomerService;
import com.cockroachlabs.services.OrderService;
import com.cockroachlabs.services.PingService;
import com.cockroachlabs.services.ProductService;
import com.cockroachlabs.util.SessionUtil;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.logging.Logger;

public class ConcurrencyTest extends AbstractConcurrencyTest{

    @Test
    public void testInflation() throws Throwable {
        Logger.getLogger("com.cockroachlabs").info("Inflate");

        Assert.assertEquals(10, 10);

        launchApp();

        Thread.sleep(5000);

        AtomicInteger uniqueCounter = new AtomicInteger();
        concurrentTest(new Inflation(uniqueCounter));
    }

    public void concurrentTest(TestRunnable... tasks) throws Throwable {
        long start = System.currentTimeMillis();
        run(tasks);
        long end = System.currentTimeMillis() - start;
        Logger.getLogger("com.cockroachlabs").info("took " + end + " ms");
    }

    private void launchApp()  {
        Runnable server = () -> {
            String[] args = {};
            try {
                Application.main(args);
            }catch(Exception e){
                e.printStackTrace();
            }
        };

        new Thread(server).start();
    }


    private class Ping implements TestRunnable {

        private final AtomicInteger clientIndex;

        public Ping(AtomicInteger clientIndex) {
            this.clientIndex = clientIndex;
        }

        @Override
        public void run() throws Throwable {
            try {
                HttpUriRequest request = new HttpGet("http://localhost:6543/ping");
                HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
                Logger.getLogger("com.cockroachlabs").info(clientIndex.getAndIncrement() +"-"+ httpResponse.getStatusLine().getStatusCode());
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private class CreateCustomer implements TestRunnable {

        private final AtomicInteger clientIndex;

        public CreateCustomer(AtomicInteger clientIndex) {
            this.clientIndex = clientIndex;
        }

        @Override
        public void run() throws Throwable {
            try {
                HttpPost request = new HttpPost("http://localhost:6543/customer");
                StringEntity params =new StringEntity("{\"id\":1,\"name\":\"bob\"} ");
                request.addHeader("content-type", "application/x-www-form-urlencoded");
                request.setEntity(params);
                HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
                Logger.getLogger("com.cockroachlabs").info(clientIndex.getAndIncrement() +"-"+ httpResponse.getStatusLine().getStatusCode());
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private class Inflation implements TestRunnable {

        private final AtomicInteger clientIndex;

        public Inflation(AtomicInteger clientIndex) {
            this.clientIndex = clientIndex;
        }

        @Override
        public void run() throws Throwable {
            try {
                HttpGet request = new HttpGet("http://localhost:6543/product/inflation/328860826330169345");
                HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
                Logger.getLogger("com.cockroachlabs").info(clientIndex.getAndIncrement() +"-"+ httpResponse.getStatusLine().getStatusCode());
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
