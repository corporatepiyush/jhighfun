package org.jhighfun.util;

import org.jhighfun.util.stream.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Piyush Katariya
 */
public class TaskStream<IN> {

    private final AbstractStreamIterator<IN> iterator;

    public TaskStream(AbstractStreamIterator<IN> iterator) {
        this.iterator = iterator;
    }

    public TaskStream(Iterator<IN> iterator) {
        this.iterator = new AbstractStreamIteratorAdapter<IN>(iterator);
    }

    public TaskStream(Iterable<IN> iterable) {
        this.iterator = new AbstractStreamIteratorAdapter<IN>(iterable);
    }

    public <INIT> TaskStream(INIT initialInput, Function<INIT, Tuple2<INIT, IN>> function, Function<INIT, Boolean> predicate) {
        this.iterator = new LazyStreamIterator<INIT, IN>(initialInput, function, predicate);
    }

    public <OUT> TaskStream<OUT> flatMap(Function<IN, Iterable<OUT>> function) {
        return new TaskStream<OUT>(new ExpansionStreamIterator<IN, OUT>(this.iterator, function));
    }

    public TaskStream<List<IN>> _batch(int batchSize) {
        return new TaskStream<List<IN>>(new BatchStreamIterator<IN>(this.iterator, batchSize));
    }

    public TaskStream<IN> _buffer(int bufferSize) {
        return new TaskStream<IN>(new BufferStreamIterator<IN>(this.iterator, bufferSize));
    }

    public <OUT> TaskStream<OUT> map(Function<IN, OUT> function) {
        return new TaskStream<OUT>(new MapperStreamIterator<IN, OUT>(this.iterator, function));
    }

    public <OUT, CARRY> TaskStream<OUT> collectWithCarryOver(CARRY initialValue, Function<Tuple2<CARRY, IN>, Tuple2<CARRY, OUT>> function) {
        return new TaskStream<OUT>(new MapperWithCarryOverStreamIterator<IN, OUT, CARRY>(this.iterator, initialValue, function));
    }

    public TaskStream<IN> filter(Function<IN, Boolean> function) {
        return new TaskStream<IN>(new ConditionalStreamIterator<IN>(this.iterator, function));
    }

    public TaskStream<IN> filterAndExecuteUnfiltered(Function<IN, Boolean> function, Task<IN> task) {
        return new TaskStream<IN>(new ConditionalStreamIterator<IN>(this.iterator, function, task));
    }

    public TaskStream<List<IN>> filterSequences(Function<List<IN>, Boolean> function) {
        return new TaskStream<List<IN>>(new ExtractorStreamIterator<IN>(this.iterator, function));
    }

    public TaskStream<IN> sortWith(Comparator<IN> comparator) {
        return new TaskStream<IN>(new SorterStreamIterator<IN>(this.iterator, comparator));
    }

    public <OUT> TaskStream<OUT> _customizedOperation(CustomizedStreamIterator<IN, OUT> customizedIterator) {
        customizedIterator.setCustomizedIterator(this.iterator);
        return new TaskStream<OUT>(customizedIterator);
    }

    public <OUT> TaskStream<OUT> transform(Function<AbstractStreamIterator<IN>, AbstractStreamIterator<OUT>> function) {
        return new TaskStream<OUT>(function.apply(this.iterator));
    }

    public TaskStream<IN> execute(Task<IN> task) {
        return new TaskStream<IN>(new ExecutorStreamIterator<IN>(this.iterator, task));
    }

    public TaskStream<IN> executeAsync(final Task<IN> task) {
        return new TaskStream<IN>(new ExecutorStreamIterator<IN>(this.iterator, new Task<IN>() {
            public void execute(final IN input) {
                FunctionUtil.executeAsync(new Runnable() {
                    public void run() {
                        task.execute(input);
                    }
                });
            }
        }));
    }

    public TaskStream<IN> executeLater(final Task<IN> task) {
        return new TaskStream<IN>(new ExecutorStreamIterator<IN>(this.iterator, new Task<IN>() {
            public void execute(final IN input) {
                FunctionUtil.executeLater(new Runnable() {
                    public void run() {
                        task.execute(input);
                    }
                });
            }
        }));
    }

    public TaskStream<IN> executeWithGlobalLock(final Task<IN> task) {
        return new TaskStream<IN>(new ExecutorStreamIterator<IN>(this.iterator, new Task<IN>() {
            public void execute(final IN input) {
                FunctionUtil.executeWithGlobalLock(new Block() {
                    public void execute() {
                        task.execute(input);
                    }
                });
            }
        }));
    }

    public TaskStream<IN> executeWithLock(final Operation operation, final Task<IN> task) {
        return new TaskStream<IN>(new ExecutorStreamIterator<IN>(this.iterator, new Task<IN>() {
            public void execute(final IN input) {
                FunctionUtil.executeWithLock(operation, new Block() {
                    public void execute() {
                        task.execute(input);
                    }
                });
            }
        }));
    }

    public TaskStream<IN> _ensureThreadSafety() {
        return new TaskStream<IN>(new ConcurrentStreamIterator<IN>(this.iterator));
    }

    public TaskStream<IN> _processExclusively() {
        final List<IN> list = new LinkedList<IN>();
        final Tuple2<String, Throwable> exception = new Tuple2<String, Throwable>("Exception", null);
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (iterator.hasNext()) {
                        list.add(iterator.next());
                    }
                } catch (Throwable e) {
                    exception._2 = e;
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (exception._2 != null) {
            throw new RuntimeException(exception._2);
        }

        return new TaskStream<IN>(list);
    }

    public TaskStream<IN> _process() {
        return new TaskStream<IN>(extract());
    }

    public CollectionFunctionChain<IN> _processAndChain() {
        return new CollectionFunctionChain<IN>(extract());
    }

    public List<IN> extract() {
        final List<IN> list = new LinkedList<IN>();
        while (this.iterator.hasNext()) {
            list.add(this.iterator.next());
        }
        return list;
    }

    public <O> O extract(Function<AbstractStreamIterator<IN>, O> extractor) {
        return extractor.apply(this.iterator);
    }

}
