/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cockroachlabs.concurrency;


import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * Slighly modified version of org.keycloak.testsuite.admin.concurrency.AbstractConcurrencyTest
 *
 */
public abstract class AbstractConcurrencyTest {

    private static final int DEFAULT_THREADS = 4;
    private static final int DEFAULT_NUMBER_OF_EXECUTIONS = 5 * DEFAULT_THREADS;


    // If enabled only one request is allowed at the time. Useful for checking that test is working.
    private static final boolean SYNCHRONIZED = false;

    protected void run(final TestRunnable... runnables) {
        run(DEFAULT_THREADS, DEFAULT_NUMBER_OF_EXECUTIONS, runnables);
    }

    protected void run(final int numThreads, final int totalNumberOfExecutions, final TestRunnable... runnables) {
        run(numThreads, totalNumberOfExecutions, this, runnables);
    }


    public static void run(final int numThreads, final int totalNumberOfExecutions, AbstractConcurrencyTest testImpl, final TestRunnable... runnables) {
        final ExecutorService service = SYNCHRONIZED
                ? Executors.newSingleThreadExecutor()
                : Executors.newFixedThreadPool(numThreads);

        AtomicInteger currentThreadIndex = new AtomicInteger();
        Collection<Callable<Void>> tasks = new LinkedList<>();
        Collection<Throwable> failures = new ConcurrentLinkedQueue<>();
        final List<Callable<Void>> runnablesToTasks = new LinkedList<>();
        for (TestRunnable runnable : runnables) {
            runnablesToTasks.add(() -> {
                int arrayIndex = currentThreadIndex.getAndIncrement() % numThreads;
                try {
                    runnable.run();
                } catch (Throwable ex) {
                    failures.add(ex);
                }
                return null;
            });
        }
        for (int i = 0; i < totalNumberOfExecutions; i ++) {
            runnablesToTasks.forEach(tasks::add);
        }

        try {
            service.invokeAll(tasks);
            service.shutdown();
            service.awaitTermination(3, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }

        if (! failures.isEmpty()) {
            RuntimeException ex = new RuntimeException("There were failures in threads. Failures count: " + failures.size());
            failures.forEach(ex::addSuppressed);
            throw ex;
        }
    }


    public interface TestRunnable {

        void run() throws Throwable;

    }

}