package com.zy.sgip.tools.socket;

import com.google.gson.JsonObject;
import com.zy.sgip.tools.bean.*;
import com.zy.sgip.tools.exception.ConnectionException;
import com.zy.sgip.tools.send.Configuration;
import com.zy.sgip.utils.LoginRedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.Socket;

/**
 * 链接到SMG对象
 * @author marker
 * */
public class Connection {

	private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

	//超时时间
	private int connectionTimeout;
	//缓冲大小
	private int ioBufferSize;
	//套接字
	protected Socket socket;
	//redis链接
	private static Jedis jedis = LoginRedis.login();
	
	protected SGIP_OutputStream out;//输出流
	protected SGIP_InputStream  in; //输入流
	
	//网关ip地址
	private String host;
	//网关端口
	private int port;


	/**
	 * Connection 单例
	 */
	private static Connection connection;

	/**
	 * @param host SMG的IP地址
	 * @param port SMG的端口
	 * */
	public Connection(String host, int port) {
		this.connectionTimeout = 30000;
		this.ioBufferSize = 2048;
		this.host = host;
		this.port = port;
	}

	public static synchronized Connection newInstance() {
		if (connection == null) {
			new Connection(Configuration.UIP, Configuration.UPORT);
		}
		return connection;
	}
	
	

	
	
	/**
	 * @param host SMG的IP地址
	 * @param port SMG的端口
	 * @param connectionTimeout 超时时间
	 * @param ioBufferSize 缓冲大小
	 * */
	public Connection(String host, int port, int connectionTimeout, int ioBufferSize) {
		this.connectionTimeout = connectionTimeout;
		this.ioBufferSize = ioBufferSize;
		this.host = host;
		this.port = port;
	}


	
	/**
	 * 打开链接
	 * */
	public synchronized void open() throws IOException {
		//this.closeNotException();// 关闭并清空socket链接
		if (socket == null || out == null || in == null) {
			socket = new Socket(host, port);
			//socket.setSoTimeout(60000);// 设置连接超时时间
			out = new SGIP_OutputStream(socket.getOutputStream(), ioBufferSize);
			in = new SGIP_InputStream(socket.getInputStream(), ioBufferSize);
		}
	}

	
	
	/**
	 * 发送命令 
	 * @param msg 消息
	 * @throws ConnectionException 
	 * @throws ConnectionException 连接异常
	 * @throws IOException 
	 * */
	public synchronized void send(Message msg) throws ConnectionException, IOException {
		if (socket == null || out == null || in == null) {
			throw new ConnectionException("connection is invalid!");
		} else {
			msg.write(out);
			out.flush();//刷新缓冲输出流
		}
	}

	
	
	/**
	 * 获取结果
	 * @return Message 消息
	 * */
	public synchronized Message recv() throws IOException {
		MsgHead head = new MsgHead();
		head.read(in);//读取消息头
		Message msg = null;
		switch (head.getCommand()) {
		case -2147483647://Bind响应命令
			msg = new BindResp();
			msg.read(in);
			break; 
		case -2147483644:
			msg = new DeliverResp();
			msg.read(in);
			break; 
		case -2147483643:
			msg = new ReportResp();
			msg.read(in);
			break; 
		case -2147483645:
			msg = new SubmitResp();
			msg.read(in);
			break; 
		case -2147483646:
			msg = new UnbindResp();
			msg.read(in);
			break;
		}
		if (msg != null) msg.setHead(head);
		try {
			SubmitResp submitResp = (SubmitResp) msg;
			LOGGER.info("-- 济南联通存储发送状态Redis -- ");
			JsonObject object = new JsonObject();
			object.addProperty("MsgId", submitResp.getSequence());
			object.addProperty("Result", submitResp.getResult());
			String key = submitResp.getSequence().substring(submitResp.getSequence().length() - 9, submitResp.getSequence().length());
			jedis.lpush("KEY_JNLT_SUBMIT_STATUS", key);
			jedis.set(key, object.toString());
			LOGGER.info("\n 济南联通的提交状态:{} {}", key, object.toString());
		} catch (Exception e) {
			//LOGGER.error("\n 济南联通的提交状态 错误 {}", e.getMessage());
		}
		return msg;
	}
	
	

	//无异常关闭连接
	public void closeNotException() {
		try {
			this.close();
		} catch (IOException exception) { }
	}

	
	//关闭连接（可能抛出异常）
	public void close() throws IOException {
		try {
			if (in     != null) in.close();//关闭输入流
			if (out    != null) out.close();//关闭输出流
			if (socket != null) { socket.close();}//关闭套接字
		} finally {
			socket = null;
		}
	}
	
	
	
	
	
	public int getIoBufferSize() {
		return ioBufferSize;
	}

	public void setIoBufferSize(int ioBufferSize) {
		this.ioBufferSize = ioBufferSize;
	}

	public boolean checkLink() {
		return !socket.isConnected() && !socket.isClosed();
	}

	//获取活动时间
	public long getLastActive(boolean isSend) {
		return isSend ? out.getLastAccess() : in.getLastAccess();
	}
}
