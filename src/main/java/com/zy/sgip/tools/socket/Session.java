package com.zy.sgip.tools.socket;

import com.google.gson.JsonObject;
import com.zy.sgip.tools.bean.*;
import com.zy.sgip.tools.exception.BindException;
import com.zy.sgip.tools.exception.ConnectionException;
import com.zy.sgip.tools.send.Configuration;
import com.zy.sgip.utils.LoginRedis;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


/**
 * 抽象网关会话类
 * @author marker
 * */
public  class Session {

	private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);

	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//网关连接对象
	private static Connection conn = Connection.newInstance();
	//redis链接
	private static Jedis jedis = LoginRedis.login();
	
	private boolean connected = false; //连接状态
	private boolean bound     = false; //bind状态
	private long freeTime = 30000;//空闲时间
    private int localPort = Configuration.IPORT;//默认本地监听端口
	
    //绑定对象备份（当网关断开连接后，重新登录使用。）
    private Bind bind_bak;
    
    //通讯节点
    private long nodeId;

	/**
	 * CmppSocketServer 单例
	 */
	private static Session session;

	public static synchronized Session newInstance() {
		if (session == null) {
			session = new Session(conn);
		}
		return session;
	}

    
	/**
	 * 构造方法初始化连接对象
	 * @param conn 连接对象
	 */
	public Session(Connection conn) {
		this.conn = conn; 
	}

	
	protected void log(int level, String msg, Throwable t) {
		System.out.println((new SimpleDateFormat("hh-mm-ss")).format(new Date())
				+ "->" + msg);
		if (t != null)
			t.printStackTrace();
	}

	/**
	 * 登录到SMG服务器
	 * @param bind 绑定命令
	 * @return BindResp 绑定结果
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws ConnectionException 
	 * */
	public synchronized BindResp open(Bind bind) throws BindException, IOException {
		//startTimer();
		if (bind_bak != null)
			closeNotException();
		if (bind_bak != bind)
			bind_bak = bind;
		try{
			Thread.sleep(1000);
			conn.open();
			connected = true;
			conn.send(bind);
			BindResp resp = (BindResp) conn.recv();
			if (resp.getResult() == 0)
				bound = true;
			else
				throw new BindException("login fiald status:"+resp.getResult());
			return resp;
		}catch (BindException exception) {
			throw exception;
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null; 
	}
	
	 Timer timer ;
	/**
	 * 发送短信
	 * @param msg 绑定命令
	 * @return BindResp 绑定结果
	 * */
	public void sendSubmit(Submit msg) throws Exception {
		//startTimer();
//		if (!isConnected() || !isBound())
//			open(bind_bak);
		try { 
			conn.send(msg);
			conn.recv();
		} catch (Exception e) {
			this.connected = false;
			bound = false;
			//Thread.sleep(50L);//睡眠1秒重新发送
			Session.this.sendSubmit(msg);
		}
	}

	
	
	public void startTimer(){
		 if(timer != null){
		 	timer.cancel();
		 }
		 timer = new Timer();
		 timer.schedule(new TimerTask() {
			public void run() {
				 Session.this.closeNotException();
			}
		}, freeTime);
		
	}
	
	/**
	 * 无异常关闭
	 * */
	public void closeNotException() {
		try {
			close();
		} catch (Exception exception) { }finally{
			timer =  null;
		}
	}

	//抛出异常关闭
	public void close() throws ConnectionException {
		connected = false;
		bound     = false;
		try {
			conn.send(new Unbind());
		} catch (IOException e) {
			LOGGER.info("发送异常");
		} finally {
			try {
				conn.close();
			} catch (IOException e) { 
				LOGGER.info("发送异常");
			}
		}
	}
	


	public void onReport(Report report) {
		LOGGER.info("----------------状态报告-------------");
		LOGGER.info("发送状态:{},序列号;{}",report.getResult(),report.getSubmitSeq());
		LOGGER.info("-- 济南联通存储下发状态Redis -- ");
		JsonObject obj = new JsonObject();
		try {
			if (StringUtils.isNotBlank(report.getSubmitSeq()) ) {
				obj.addProperty("Report_Stat", report.getResult());
				obj.addProperty("Report_Done_time", format.format(new Date()));
				jedis.lpush("KEY_JNLT_SEND_STATUS", report.getSubmitSeq());
				jedis.set(report.getSubmitSeq(), obj.toString());
				jedis.expire(report.getSubmitSeq(),60 * 60 * 24 * 5);
				LOGGER.info("\n济南联通发送状态 :{}, {}", report.getSubmitSeq(), obj.toString());
			}
		} catch (Exception e) {
			LOGGER.error("济南联通发送状态 异常：{}",e.getMessage());
		}

	}

	public void onMessage(Deliver deliver) {
		LOGGER.info("----------------上行回复---------------");
		try {
			LOGGER.info("手机:{},短信内容:{}", deliver.getUserNumber(), new String(deliver.getContent(), "UTF-8"));
			jedis.lpush("KEY_JNLT_REPLY", deliver.getUserNumber());
			jedis.set(deliver.getUserNumber(), new String(deliver.getContent(), "GBK"));
			jedis.expire(deliver.getUserNumber(), 60 * 60 * 24 * 3);
			LOGGER.info("\n 济南联通 保存REDIS缓存成功");

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void onSubmit(SubmitResp sresp){
		LOGGER.info("-- 聚通达移动存储发送状态Redis -- ");
		JsonObject object = new JsonObject();
		try {
			Jedis jedis = LoginRedis.login();
			object.addProperty("MsgId", sresp.getSequence());
			object.addProperty("Result", sresp.getResult());
			String key = sresp.getSequence().substring(sresp.getSequence().length() - 9, sresp.getSequence().length());
			jedis.lpush("KEY_JNLT_SUBMIT_STATUS", key);
			jedis.set(key, object.toString());
			LOGGER.info("\n 济南联通的提交状态:{} {}", key, object.toString());
		} catch (Exception e) {
			LOGGER.error("\n 济南联通的提交状态 错误 {}" , e.getMessage());
		}
	}

	public void onTerminate() {
		LOGGER.info("------------断开连接--------------");
	}

	public boolean isBound() {
		return bound;
	}

	public boolean isConnected() {
		return connected;
	}




	public int getLocalPort() {
		return localPort;
	}
	/**
	 * 本地监听短信
	 * @param localPort 端口
	 * */
	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}


	public long getFreeTime() {
		return freeTime;
	}




	public void setFreeTime(long freeTime) {
		this.freeTime = freeTime;
	}




	public long getNodeId() {
		return nodeId;
	}




	public void setNodeId(long nodeId) {
		this.nodeId = nodeId;
	}
 
	
	
}
