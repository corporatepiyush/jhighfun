package org.jhighfun.internal;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ThreadPoolFactory {

    public static ExecutorService getThreadPool() {
        Context context = null;
        try {
            context = new InitialContext();
        } catch (NamingException e) {
            System.err.println("Error while looking up for 'org.jhighfun.threadpool' system property, falling back to default ThreadPool.");
        }

        if (context != null) {
            ExecutorService managedThreadPool = null;
            try {
                managedThreadPool = (ExecutorService) context.lookup("java:/comp/env/" + System.getProperty("org.jhighfun.threadpool"));
            } catch (Exception e) {
                System.err.println("Error while looking up for 'org.jhighfun.threadpool' system property, falling back to default ThreadPool.");
            } finally {
                if (managedThreadPool != null)
                    return managedThreadPool;
                else
                    return getDefaultThreadPool();
            }

        } else {
            return getDefaultThreadPool();
        }

    }

    private static ExecutorService getDefaultThreadPool() {
        final ThreadPoolExecutor pool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5, TimeUnit.MINUTES, new SynchronousQueue<Runnable>());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                pool.shutdownNow();
            }
        });

        return pool;
    }

}