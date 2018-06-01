package com.zy.sgip.tools.thread;

import com.zy.sgip.tools.bean.*;
import com.zy.sgip.tools.socket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;


/**
 * 当本地服务接收到Socket时，就创建这个线程
 * @author marker
 * */
public class HandleThread extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(HandleThread.class);

	private Socket socket;
	private Session session;
	public HandleThread(Socket socket, Session ssn) {
		this.socket = socket;
		this.session    = ssn;
	}
	
	public void run() {
		DataInputStream   in = null;
		DataOutputStream out = null;
		try {
			//ServerSocket 的绑定状态 
			in  = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			boolean terminal = false;
			while(!terminal) {
				MsgHead head = new MsgHead();
				head.read(in);
				switch (head.getCommand()) {
					case Message.SGIP_SUBMIT_RESP:
						SubmitResp submitResp = new SubmitResp();
						submitResp.read(in);
						session.onSubmit(submitResp);
						break;
					case Message.SGIP_CONNECT:
						Bind bind = new Bind();
						bind.read(in);
						int result = 0;
						BindResp resp = new BindResp(head);
						resp.setResult(result);
						resp.write(out);
						break;

					case Message.SGIP_DELIVER:
						Deliver msg = new Deliver();
						msg.setHead(head);//设置消息头
						msg.read(in);
						(new DeliverResp(head)).write(out);
						session.onMessage(msg);
						break;

					case Message.SGIP_REPORT:
						Report rpt = new Report();
						rpt.read(in);
						(new ReportResp(head)).write(out);
						session.onReport(rpt);
						break;

					case Message.SGIP_TERMINATE:
						terminal = true;//放弃连接
						Unbind ub = new Unbind();
						ub.read(in); 
						UnbindResp unbindresp = new UnbindResp(head);
						unbindresp.write(out);
						session.onTerminate();
						break;
					default: continue;
				}
			}
			return;
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.info("与SMG服务器链接异常");
			//System.out.println("与SMG服务器链接异常");
		} finally {
			try {
				if (in  != null) in.close(); 
				if (out != null) out.close();
				if (socket != null)  socket.close(); 
			} catch (Exception exception2) {exception2.printStackTrace(); }
		}
	}
}