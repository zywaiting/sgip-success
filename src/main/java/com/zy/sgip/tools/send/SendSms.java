package com.zy.sgip.tools.send;

import com.zy.sgip.tools.bean.Submit;
import com.zy.sgip.tools.socket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class SendSms {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendSms.class);

    private static Session session = Session.newInstance();

    public static void sendSms(String mobile, String content, String extNo, String id) throws Exception {




        String SPNumber = Configuration.SPCODE + extNo; //接入号+extNo扩展码
        String ChargeNumber = "000000000000000000000";//付费号码
        String[] UserNumber = mobile.split(",");//拆分手机号码86开头
        String CorpId = Configuration.SENDSCRIPT;//企业代码
        String ServiceType = Configuration.URL;//业务代码
        int FeeType = 2;//计费类型
        int FeeValue = 0;//该条短消息的收费值
        int GivenValue = 0;//赠送用户的话费
        int AgentFlag = 1;//代收费标志
        int MorelatetoMTFlag = 3;//引起MT消息的原因
        int Priority = 0;//优先级
        String ExpireTime = null;//短消息寿命的终止时间
        String ScheduleTime = null;//短消息定时发送的时间
        int ReportFlag = 1;//状态报告标记---1-该条消息无论最后是否成功都要返回状态报告
        int MessageType = 0;//信息类型
        int TP_pid = 0;//GSM协议类型
        int TP_udhi = 0;//GSM协议类型
        int MessageCoding = 15;//短消息的编码格式---15：GBK编码
        byte[] MessageContent = null;//短消息的内容
        try {
            MessageContent = content.getBytes("GBK");//"您本次的验证码是：1234".getBytes("GBK");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        int MessageLen = MessageContent.length;
        String reserve = "";//保留，扩展用
        long nodeid = Configuration.REPORTSCRIPT;//通讯节点

        Submit s = new Submit(TP_pid, TP_udhi, SPNumber, ChargeNumber, UserNumber, CorpId,
                nodeid, ServiceType, FeeType, FeeValue, GivenValue, AgentFlag,
                MorelatetoMTFlag, Priority, ExpireTime, ScheduleTime,
                ReportFlag, MessageCoding, MessageType, MessageContent,
                MessageLen, reserve, id);
        session.sendSubmit(s);
    }

    public static void main(String[] args) throws Exception {
        String mobile ="8617605674666";
        String content = "【信用卡中心】推荐您申请浦发信用卡，消费送好礼！戳 http://t.cn/RujdpR6 最终以审核为准，已办卡请忽略。退订回T";
        String extNo = "98888";
        String id = "123454321";
        sendSms(mobile,content,extNo,id);
    }

}
