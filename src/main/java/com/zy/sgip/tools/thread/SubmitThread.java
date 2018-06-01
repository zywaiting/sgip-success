package com.zy.sgip.tools.thread;

import com.google.gson.JsonObject;
import com.zy.sgip.tools.bean.Message;
import com.zy.sgip.tools.bean.SubmitResp;
import com.zy.sgip.tools.send.Configuration;
import com.zy.sgip.tools.socket.Connection;
import com.zy.sgip.utils.LoginRedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;


public class SubmitThread extends Thread{

    private static final Logger LOGGER = LoggerFactory.getLogger(HandleThread.class);


    public void run() {
        submitRep();
    }

    public static void submitRep(){
        LOGGER.info("----测试---");
        Connection conn = new Connection(Configuration.UIP, Configuration.UPORT);
        Message message = null;
        try {
            message = conn.recv();
            if (message != null) {
                SubmitResp recv = (SubmitResp) message;
                try {
                    LOGGER.info("-- 聚通达移动存储发送状态Redis -- ");
                    JsonObject object = new JsonObject();
                    Jedis jedis = LoginRedis.login();
                    object.addProperty("MsgId", recv.getSequence());
                    object.addProperty("Result", recv.getResult());
                    String key = recv.getSequence().substring(recv.getSequence().length() - 9, recv.getSequence().length());
                    jedis.lpush("KEY_JNLT_SUBMIT_STATUS", key);
                    jedis.set(key, object.toString());
                    LOGGER.info("\n 济南联通的提交状态:{} {}", key, object.toString());
                } catch (Exception e) {
                    LOGGER.error("\n 济南联通的提交状态 错误 {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.info("未获取到重新获取");
            try {
                Thread.sleep(1000);
                submitRep();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    }
}
