package com.wen.test;

/**
 * Created by wen on 2017/3/8.
 */
public class NotifyTest {
    private String flag[] = { "true" };
    private int num = 0;
    class NotifyThread extends Thread {
        public NotifyThread(String name) {
            super(name);
        }
        public void run() {
            int index = 0;
            while(index <= 200)
            {
                synchronized (flag) {
                    if(flag[0].equals("true"))
                    {
                        try {
                            System.out.println("通知线程即将陷入wait...");
                            flag.wait();
                            System.out.println("通知线程脱离wait...");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    flag.notifyAll();
                    flag[0] = "true";
                    System.out.println("等待..." + index);
                }
               //
                ++index;
            }
        }
    };

    class WaitThread extends Thread {
        public WaitThread(String name) {
            super(name);
        }
        public void run() {
            int index = 0;
            while(index <= 200)
            {
                synchronized (flag) {

                    if(flag[0].equals("false"))
                    {
                        try {
                            System.out.println("等待线程即将陷入wait...");
                            flag.wait();
                            System.out.println("等待线程脱离wait...");

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    flag.notifyAll();
                    flag[0] = "false";

                }
                System.out.println("通知..." + index);
                ++index;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Main Thread Run!");
        NotifyTest test = new NotifyTest();
        NotifyThread notifyThread = test.new NotifyThread("notify01");
        WaitThread waitThread01 = test.new WaitThread("waiter01");
        notifyThread.start();
        waitThread01.start();
    }

}
