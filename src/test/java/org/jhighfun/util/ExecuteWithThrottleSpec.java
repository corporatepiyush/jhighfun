package org.jhighfun.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ExecuteWithThrottleSpec {

    private static ConcurrentHashMap<ExecutionThrottler, ExecutorService> mapSpy;
    private static ExecutionThrottler throttler;

    public static void init() throws Exception {

        Field throttlerPoolMap = FunctionUtil.class.getDeclaredField("throttlerPoolMap");
        throttlerPoolMap.setAccessible(true);
        mapSpy = spy(new ConcurrentHashMap<ExecutionThrottler, ExecutorService>(15, 0.9f, 32));
        throttlerPoolMap.set(null, mapSpy);

        String identity = "some operation";
        throttler = FunctionUtil.throttler(identity);
        FunctionUtil.registerPool(throttler, 10);

    }

    @Test
    public void testExecuteWithThrottle() throws Exception {
        init();
        Block codeBlockMock = mock(Block.class);
        FunctionUtil.executeWithThrottle(throttler, codeBlockMock);

        verify(mapSpy, times(1)).get(throttler);
        verify(codeBlockMock, times(1)).execute();

    }

    @Test
    public void testExecuteAsyncWithThrottle() throws Exception {
        init();
        Block codeBlockMock = mock(Block.class);
        FunctionUtil.executeAsyncWithThrottle(throttler, codeBlockMock);

        verify(mapSpy, times(1)).get(throttler);
        Thread.sleep(200);

        verify(codeBlockMock, times(1)).execute();

    }

    @Test
    public void testExecuteAsyncWithThrottle_callback() throws Exception {
        init();
        AsyncTask<Object> asyncTask = mock(AsyncTask.class);
        CallbackTask<Object> callbackTask = mock(CallbackTask.class);


        Object asyncTaskResult = new Object();
        when(asyncTask.execute()).thenReturn(asyncTaskResult);

        FunctionUtil.executeAsyncWithThrottle(throttler, asyncTask, callbackTask);

        verify(mapSpy, times(1)).get(throttler);
        Thread.sleep(200);


        verify(asyncTask, times(1)).execute();
        ArgumentCaptor<AsyncTaskHandle> argument = ArgumentCaptor.forClass(AsyncTaskHandle.class);
        verify(callbackTask, times(1)).execute(argument.capture());

        assertEquals(argument.getValue().getAsyncTask(), asyncTask);
        assertEquals(argument.getValue().getOutput(), asyncTaskResult);
        assertEquals(argument.getValue().getException(), null);
    }
}