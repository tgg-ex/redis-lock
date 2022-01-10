package com.tgg.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController()
public class TestDemoController {

    @Autowired
    private RedisLock redisLock;

    @RequestMapping("/demo")
    public String demo() throws Exception {


        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                int count = 0;

                boolean test1 = redisLock.tryLock("test:lock", "111111", 2);
                if (test1) {
                    System.out.println(" ========================加锁成功test1 = " + test1);
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                redisLock.releaseLock("test:lock", "111111");
            }

        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub

                while (true) {
                    boolean test1 = redisLock.tryLock("test:lock", "111111", 2);
                    if (test1) {
                        System.out.println("+++++++++++++++++++++ 加锁成功test2 = " + test1);
                        redisLock.releaseLock("test:lock", "111111");
                        return;
                    } else {
                        System.out.println("++++++++++++++++++++++++等待锁 = " + test1);
                    }
                }


            }

        }).start();
        return "ok";
    }
}
