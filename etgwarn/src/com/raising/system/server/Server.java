package com.raising.system.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 顺安居报警系统服务器端
 * 
 * @author bail
 * 
 */
public class Server{
	static String URL = "jdbc:mysql://112.124.60.128:3306/etg_db";
	static String USERNAME = "root";
	static String PASSWORD = "raising";
	static {
		try {
			// 加载MySql的驱动类
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	Connection con = null;
	Statement stmt = null;
	ResultSet rs = null;
	
	static ServerSocket ss;
	Socket socket;
	/** 客户端的输入流 */
	InputStream inStream;
	/** 客户端的输出流 */
	OutputStream outStream;
	
	public Server(){}
	/**
	 * 初始化套接字,输入输出流数据
	 */
	public Server(Socket s){
		try {
			socket = s;
			inStream = socket.getInputStream();// 获取输入流
			outStream = socket.getOutputStream();// 获取输出流
		} catch (Exception e) {
			System.out.println("服务端初始化输入输出流出现故障！");
			e.printStackTrace();
		}
	}
	
	/**
	 * 用于服务器发送指令使用
	 */
	public void serverSay() {
		new Thread() {
			public void run() {
				try {
					while (true) {
						byte[] outArr = new byte[100];
						System.in.read(outArr);
						outStream.write(outArr);
						System.out.println("服务器说:--->>" + new String(outArr).toString());
						outStream.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}finally{
					try{
						outStream.close();
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	/**
	 * 在程序中自动发送初始化指令
	 */
	public void serverSayCMD() {
		new Thread() {
			public void run() {
				try {
					//开始运行时依次执行以下语句，实现部署
					//撤防指令
					outStream.write("AT+CDAM=1".getBytes());
					outStream.flush();
					System.out.println("服务器说:--->>AT+CDAM=1");
					Thread.sleep(500);
					//撤防确认
					outStream.write("AT+CDAM=1".getBytes());
					outStream.flush();
					System.out.println("服务器说:--->>AT+CDAM=1");
					Thread.sleep(500);
					//布防指令
					outStream.write("AT+CARM=1".getBytes());
					outStream.flush();
					System.out.println("服务器说:--->>AT+CARM=1");
					Thread.sleep(500);
					//布防确认
					outStream.write("AT+CARM=1".getBytes());
					outStream.flush();
					System.out.println("服务器说:--->>AT+CARM=1");
					Thread.sleep(500);
					//异步指令
					outStream.write("AT+CWMSG=SET,0,1".getBytes());
					outStream.flush();
					System.out.println("服务器说:--->>AT+CWMSG=SET,0,1");
					
				} catch (Exception e) {
					System.out.println("在程序中自动发送初始化指令时出现故障！");
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	/**
	 * 监听报警客户端发出的信息,并解析处理
	 */
	public void listen() {
		new Thread() {
			public void run() {
				try {
					DataInputStream dis = new DataInputStream(inStream);
					String inMessage = "";// 客户端回复
					String messId = null;//消息id
					String areaId = null;//防区号
					byte [] buf;//缓存数组
					int len;//读取字节
					while (true) {
						buf=new byte[1024];
						len=dis.read(buf);
						inMessage = new String(buf,0,len);
						System.out.print("报警主机客户端 说:<<---");
						System.out.println(inMessage);
						if (inMessage.indexOf("DFA_ALARM") > -1) {
							dbOperate();
							// 获取消息id
							messId = inMessage.substring(inMessage.indexOf("+CWMSG:") + 7,inMessage.indexOf(','));
							// 获取防区号
							areaId = inMessage.substring(inMessage.lastIndexOf(',') + 1);
							inMessage = "第" + messId + "条消息,防区" + areaId+ ",报警了";
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					try{
						inStream.close();
						if(socket.isClosed()){
							System.out.println("客户端中断了socket");
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	/**
	 * 数据库操作(包括查询是否需要报警,是否有未处理的登船记录,进行更新登船记录操作或插入报警表插座,提示音)
	 */
	public void dbOperate() throws Exception{
		try {
			con = DriverManager.getConnection(URL,USERNAME, PASSWORD);
			stmt = con.createStatement();
			// 查询是否布防状态 1:布防需要报警 0:撤防不需要报警
			rs = stmt.executeQuery("SELECT t.PARAMVAL FROM SYS_PARAM t WHERE t.PARAMID = 1");
			if (rs.next()) {
				String isDe = rs.getString(1);
				if ("1".equals(isDe)) {
					// 查询没有处理的登船记录
					rs = stmt.executeQuery("SELECT bs.SteamerUpDown_Id as id FROM BUS_STEAMERUpDown bs WHERE bs.Deal_Yn = 0 AND TO_SECONDS(bs.Enter_Tm) > TO_SECONDS(SYSDATE()-100) ORDER BY SteamerUpDown_Id DESC LIMIT 1");
					// 如果存在未处理的登船记录(并且一分钟以内的)，则更新成已处理状态
					if (rs.next()) {
						playMyMusic(PlayMusic.PASS_MUSIC);//单独启动一个线程发出通过提示音
						int id = rs.getInt(1);// 取出没有处理的一条记录
						stmt.executeUpdate("UPDATE BUS_STEAMERUPDOWN b set b.Deal_Yn = 1 WHERE b.SteamerUpDown_Id = "+ id);
					} else {
						playMyMusic(PlayMusic.WARN_MUSIC);//单独启动一个线程发出报警提示音
						rs = stmt.executeQuery("SELECT t.Steamer_Id FROM pty_steamerelecsentry t WHERE t.ElecSentry_Cd = 01");
						if(rs.next()){
							String Id = rs.getString(1);
							// 如果都是已经处理的登船记录，则插入警报信息
							stmt.executeUpdate("INSERT INTO BUS_STEAMERWARNSTAFF (steamer_id) VALUES('"+Id+"')");
						}
					}
				}
			}
		} catch (SQLException se) {
			System.out.println("数据库连接失败！");
			se.printStackTrace();
		} finally {
			if(rs != null){
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (stmt != null) { // 关闭声明
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (con != null) { // 关闭连接对象
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 启动单独的线程播放音频
	 */
	public void playMyMusic(final String fName){
		new Thread(){
			public void run() {
				PlayMusic.play(fName);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static void main(String args[]) throws Exception {
		System.out.println("====================创建服务器端，192.168.1.230侦听8099端口==================");
		ss = new ServerSocket(8099);
		while(true){
			Socket s = ss.accept();
			System.out.println("有客户连接上了：" + s.getInetAddress() + ":" + s.getPort());
			Server server = new Server(s);
			server.listen();
			server.serverSay();
			server.serverSayCMD();
		}
		
//		long b = System.currentTimeMillis();
//		new Server().dbOperate();
//		System.out.println(System.currentTimeMillis()-b);//测试数据库以及音频消耗时间
	}
}