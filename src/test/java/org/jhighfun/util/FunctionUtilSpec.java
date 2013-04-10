package org.jhighfun.util;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.jhighfun.util.CollectionUtil.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class FunctionUtilSpec {

    @Spy
    ExecutorService spyMediumPriorityAsyncTaskThreadPool = new ThreadPoolExecutor(0, 100, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Spy
    ExecutorService spyLowPriorityAsyncTaskThreadPool = new ThreadPoolExecutor(0, 5, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Before
    public void before() {
        try {

            Field globalPool = FunctionUtil.class.getDeclaredField("mediumPriorityAsyncTaskThreadPool");
            globalPool.setAccessible(true);
            globalPool.set(null, spyMediumPriorityAsyncTaskThreadPool);

            globalPool = FunctionUtil.class.getDeclaredField("lowPriorityAsyncTaskThreadPool");
            globalPool.setAccessible(true);
            globalPool.set(null, spyLowPriorityAsyncTaskThreadPool);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testChain() {

        Set<String> set = new HashSet<String>();
        set.add("Scala");
        set.add("Java");

        CollectionFunctionChain<String> chain = FunctionUtil.chain(set);

        Collection<String> expected = chain.extract();

        assertEquals(set, expected);
    }

    @Test
    public void testCurry() {

        CurriedFunction<Integer, Integer> addToFive = FunctionUtil.curry(new Function<Integer, Integer>() {
            public Integer execute(Collection<Integer> integers) {
                int sum = 0;
                for (Integer i : integers) {
                    sum = sum + i;
                }
                return sum;
            }
        }, List(5));

        assertTrue(addToFive.call(List(10)) == 15);
        assertTrue(addToFive.call(List(10, 15)) == 30);
        assertTrue(addToFive.call(10) == 15);
        assertTrue(addToFive.call(10, 15) == 30);

        CurriedFunction<Integer, Integer> addToZero = FunctionUtil.curry(new Function<Integer, Integer>() {
            public Integer execute(Collection<Integer> integers) {
                int sum = 0;
                for (Integer i : integers) {
                    sum = sum + i;
                }
                return sum;
            }
        }, 0, 0);

        assertTrue(addToZero.call(List(10)) == 10);
        assertTrue(addToZero.call(List(15)) == 15);
        assertTrue(addToZero.call(List(0)) == 0);
        assertTrue(addToZero.call(5) == 5);
        assertTrue(addToZero.call(5, 10) == 15);
    }

    @Test
    public void testMemoizeForConverter() {

        final List<String> spyInjection = new LinkedList<String>();
        final String inputCheckValue = "today";
        final Date outputCheckValue = new Date();

        Converter<String, Date> memoizedFunction = FunctionUtil.memoize(new Converter<String, Date>() {

            public Date convert(String input) {
                spyInjection.add(input);
                return input.equals("today") ? outputCheckValue : null;
            }
        });

        assertEquals(spyInjection.size(), 0);
        assertEquals(memoizedFunction.convert(inputCheckValue), outputCheckValue);
        assertEquals(spyInjection.size(), 1);

        assertEquals(memoizedFunction.convert(inputCheckValue), outputCheckValue);
        assertEquals(spyInjection.size(), 1);
    }

    @Test
    public void testMemoizeForCondition() {

        final List<String> spyInjection = new LinkedList<String>();
        final String inputCheckValue = "today";

        Predicate<String> memoizedFunction = FunctionUtil.memoize(new Predicate<String>() {

            public boolean evaluate(String input) {
                spyInjection.add(input);
                return input.equals("today") ? true : false;
            }
        });

        assertEquals(spyInjection.size(), 0);
        assertEquals(memoizedFunction.evaluate(inputCheckValue), true);
        assertEquals(spyInjection.size(), 1);

        assertEquals(memoizedFunction.evaluate(inputCheckValue), true);
        assertEquals(spyInjection.size(), 1);
    }

    @Test
    public void testMemoizeForFunction() {

        final List<String> spyInjection = new LinkedList<String>();

        Function<String, String> memoizedFunction = FunctionUtil.memoize(new Function<String, String>() {

            public String execute(Collection<String> args) {
                spyInjection.add(args.toString());
                StringBuilder builder = new StringBuilder();
                for (String string : args) {
                    builder.append(string);
                }
                return builder.toString();
            }
        });

        assertEquals(spyInjection.size(), 0);
        assertEquals(memoizedFunction.execute(List("I", "am", "the", "Almighty")), "IamtheAlmighty");
        assertEquals(spyInjection.size(), 1);

        assertEquals(memoizedFunction.execute(List("I", "am", "the", "Almighty")), "IamtheAlmighty");
        assertEquals(spyInjection.size(), 1);
    }


    @Test
    public void testMemoizeForAccumulator() {

        final List<String> spyInjection = new LinkedList<String>();

        Accumulator<String, String> memoizedFunction = FunctionUtil.memoize(new Accumulator<String, String>() {

            public String accumulate(String accum, String element) {
                spyInjection.add(element);
                StringBuilder builder = new StringBuilder();
                builder.append(accum).append(element);
                return builder.toString();
            }
        });

        assertEquals(spyInjection.size(), 0);
        assertEquals(memoizedFunction.accumulate("Java", "Rocks!"), "JavaRocks!");
        assertEquals(spyInjection.size(), 1);

        assertEquals(memoizedFunction.accumulate("Java", "Rocks!"), "JavaRocks!");
        assertEquals(spyInjection.size(), 1);
    }

    @Test
    public void testExecuteAsync() {

        Block mockBlock = mock(Block.class);

        FunctionUtil.executeAsync(mockBlock);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(mockBlock, times(1)).execute();
        verify(spyMediumPriorityAsyncTaskThreadPool, times(1)).submit(any(Runnable.class));
    }

    @Test
    public void testExecuteLater() {

        Block mockBlock = mock(Block.class);

        FunctionUtil.executeLater(mockBlock);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(mockBlock, times(1)).execute();
        verify(spyLowPriorityAsyncTaskThreadPool, times(1)).submit(any(Runnable.class));
    }


    @Test
    public void testExecuteWithGlobalLockWithSingleThread() {

        Block mockBlock = mock(Block.class);

        FunctionUtil.executeWithGlobalLock(mockBlock);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(mockBlock, times(1)).execute();
    }

    @Test
    public void testExecuteWithGlobalLockWithMultipleThread() {

        final List<Integer> list = new LinkedList<Integer>();

        List<Integer> load = List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        for (int i = 0; i < 10000; list.add(i), i++) ;

        final Block spyBlock = spy(new Block() {
            public void execute() {
                list.add(1);
                for (Integer i : list) ;
                list.add(2);
            }
        });

        FunctionUtil.each(load, new RecordProcessor<Integer>() {
            public void process(Integer item) {
                FunctionUtil.executeWithGlobalLock(spyBlock);
            }
        }, FunctionUtil.parallel(10));

        verify(spyBlock, times(10)).execute();

    }

    @Test
    public void testTuples() {

        String first = "first";
        Integer second = 2;
        Double third = 3.0;
        Object fourth = new Object();
        Float fifth = 5.5f;

        Pair<String, Integer> pair = FunctionUtil.tuple(first, second);
        assertEquals(pair._1, "first");
        assertEquals(pair._2, new Integer(2));

        Triplet<String, Integer, Double> triplet = FunctionUtil.tuple(first, second, third);
        assertEquals(triplet._1, "first");
        assertEquals(triplet._2, new Integer(2));
        assertEquals(triplet._3, new Double(3));

        Quadruplet<String, Integer, Double, Object> quadruplet = FunctionUtil.tuple(first, second, third, fourth);
        assertEquals(quadruplet._1, "first");
        assertEquals(quadruplet._2, new Integer(2));
        assertEquals(quadruplet._3, new Double(3));
        assertEquals(quadruplet._4, fourth);


        Quintuplet<String, Integer, Double, Object, Float> quintuplet = FunctionUtil.tuple(first, second, third, fourth, fifth);
        assertEquals(quintuplet._1, "first");
        assertEquals(quintuplet._2, new Integer(2));
        assertEquals(quintuplet._3, new Double(3));
        assertEquals(quintuplet._4, fourth);
        assertEquals(quintuplet._5, fifth);

    }

}
