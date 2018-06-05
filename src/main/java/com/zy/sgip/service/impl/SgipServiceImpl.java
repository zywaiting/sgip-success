package com.zy.sgip.service.impl;

import com.zy.sgip.service.SgipService;
import com.zy.sgip.tools.bean.Bind;
import com.zy.sgip.tools.bean.BindResp;
import com.zy.sgip.tools.exception.BindException;
import com.zy.sgip.tools.send.Configuration;
import com.zy.sgip.tools.send.SendSms;
import com.zy.sgip.tools.socket.Connection;
import com.zy.sgip.tools.socket.Session;
import com.zy.sgip.tools.thread.ListenThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Service
@EnableScheduling
public class SgipServiceImpl implements SgipService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SgipServiceImpl.class);
    private static Session session = Session.newInstance();


    private ListenThread listenThread = null;//上行、状态报告监听器


    @PostConstruct
    public void init() {
        try {
            Connection conn = new Connection(Configuration.UIP, Configuration.UPORT);
            Session session = new Session(conn);
            if (listenThread == null) {
                listenThread = new ListenThread(session);
                listenThread.start();
            }
        } catch (Exception e){

        }
    }


    /**
     * 短信发送
     */
    @Override
    @Transactional
    public String smsSend(HttpServletRequest request) {
        try {
            String mobile = request.getParameter("mobile");
            String content = request.getParameter("content");
            String extNo = request.getParameter("extNo");
            String id = request.getParameter("sequenceId");
            SendSms.sendSms(mobile,content,extNo,id);
        } catch (Exception e){
            LOGGER.info("出现异常:{}",e.getMessage());
            e.printStackTrace();
        }
        return "success";
    }


    @Scheduled(cron = "0/40 * * * * ?")
    public void Socket() {
        Connection conn = new Connection(Configuration.UIP,Configuration.UPORT);
        try {
            LOGGER.info("心跳包");
            Bind bind = new Bind(1, Configuration.NAME, Configuration.PASSWORD, Configuration.REPORTSCRIPT);
            LOGGER.info("-----------登陆中-----------");
            session.setLocalPort(Configuration.IPORT);
            BindResp resp = session.open(bind);
            LOGGER.info("登录状态;{}",resp.getResult());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BindException e) {
            e.printStackTrace();
        }
    }
}
