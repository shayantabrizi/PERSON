/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.ac.ut.iis.person.base;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author shayan
 */
public class Threads {

    private static int tokenIndex;
    private static List<MyThread> list;
    private static Throwable exceptionCause;

    public static void addRunnable(Runnable run, String name) {
        if (list == null) {
            list = new ArrayList<>();
        }
        MyThread th = new MyThread(run, name);
        th.state = State.WAITING;
        list.add(th);
    }

    public static void enterUnsafeArea() throws RuntimeException {
        if (list != null && !list.isEmpty()) {
            synchronized (Threads.class) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new ThreadStoppedException();
                }
                MyThread myThread = (MyThread) Thread.currentThread();
                if (myThread.state.equals(Threads.State.UNSAFE_RUN)) {
                    return;
                }
                if (myThread.state.equals(Threads.State.SAFE_RUN)) {
                    myThread.state = Threads.State.WAITING;
                }
                try {
                    while (myThread.state.equals(Threads.State.WAITING)) {
                        Threads.class.wait();
                    }
                    myThread.state = Threads.State.UNSAFE_RUN;
                } catch (InterruptedException ex) {
//                    Logger.getLogger(Threads.class.getName()).log(Level.SEVERE, null, ex);
                    throw new ThreadStoppedException();
                }
                int cnt = 0;
                for (MyThread th : list) {
                    if (th.state.equals(Threads.State.UNSAFE_RUN)) {
                        cnt++;
                    }
                }
                if (cnt > 1) {
                    throw new RuntimeException();
                }
            }
        }
    }

    public static void start() {
        if (list != null && !list.isEmpty()) {
            synchronized (Threads.class) {
                exceptionCause = null;
                list.get(0).state = Threads.State.UNSAFE_RUN;
                tokenIndex = 0;
                for (MyThread t : list) {
                    t.start();
                }
            }
        }
    }

    public static void enterSafeArea() {
        if (list != null && !list.isEmpty()) {
            synchronized (Threads.class) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new ThreadStoppedException();
                }
                MyThread myThread = (MyThread) Thread.currentThread();
                if (myThread.state.equals(Threads.State.UNSAFE_RUN)) {
                    myThread.state = Threads.State.SAFE_RUN;
                    for (int i = 0; i < list.size(); i++) {
                        MyThread tokenThread = list.get(tokenIndex);
                        tokenIndex = (tokenIndex + 1) % list.size();
                        if (tokenThread.state.equals(Threads.State.WAITING)) {
                            tokenThread.state = Threads.State.UNSAFE_RUN;
                            Threads.class.notifyAll();
                            int cnt = 0;
                            for (MyThread th : list) {
                                if (th.state.equals(Threads.State.UNSAFE_RUN)) {
                                    cnt++;
                                }
                            }
                            if (cnt > 1) {
                                throw new RuntimeException();
                            }
                            return;
                        }
                    }

                    for (int i = 0; i < list.size(); i++) {
                        MyThread tokenThread = list.get(tokenIndex);
                        tokenIndex = (tokenIndex + 1) % list.size();
                        if (tokenThread.state.equals(Threads.State.SAFE_RUN)) {
                            tokenThread.state = Threads.State.UNSAFE_RUN;
                            int cnt = 0;
                            for (MyThread th : list) {
                                if (th.state.equals(Threads.State.UNSAFE_RUN)) {
                                    cnt++;
                                }
                            }
                            if (cnt > 1) {
                                throw new RuntimeException();
                            }
                            return;
                        }
                    }
                }
            }
        }
    }

    public static void join() {
        if (list != null && !list.isEmpty()) {
            for (MyThread th : list) {
                try {
                    th.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Threads.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException();
                }
            }
            list.clear();
        }
        if (exceptionCause != null) {
            throw new IgnoreQueryEx(exceptionCause.getMessage());
        }
    }

    public static class MyThread extends Thread {

        final Runnable runnable;
        final String name;
        Threads.State state;

        public MyThread(Runnable runnable, String name) {
            this.runnable = runnable;
            this.name = name;
        }

        @Override
        public void run() {
            try {
                Threads.enterUnsafeArea();
                try {
                    runnable.run();
                } catch (IgnoreQueryEx e) {
//                    Logger.getLogger(Threads.class.getName()).log(Level.SEVERE, null, e);
                    synchronized (Threads.class) {
                        for (MyThread th : list) {
                            if (th.isAlive()) {
                                th.interrupt();
                            }
                        }
                        throw new ThreadStoppedException(e);
                    }
                } catch (ThreadStoppedException e) {
                    throw e;
                } catch (Throwable t) {
                    Logger.getLogger(Threads.class.getName()).log(Level.SEVERE, null, t);
                    System.exit(1);
                }
                synchronized (Threads.class) {
                    Threads.enterSafeArea();
                    state = Threads.State.FINISHED;
                }
            } catch (ThreadStoppedException e) {
                if (e.getCause() != null) {
                    Threads.exceptionCause = e.getCause();
                }
            }
        }
    }

    private static enum State {
        WAITING,
        UNSAFE_RUN,
        SAFE_RUN,
        FINISHED
    }

    public static class ThreadStoppedException extends RuntimeException {

        public ThreadStoppedException() {
        }

        public ThreadStoppedException(Throwable cause) {
            super(cause);
        }

    }
}
