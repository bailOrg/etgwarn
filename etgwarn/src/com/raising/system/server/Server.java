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
 * ˳���ӱ���ϵͳ��������
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
			// ����MySql��������
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
	/** �ͻ��˵������� */
	InputStream inStream;
	/** �ͻ��˵������ */
	OutputStream outStream;
	
	public Server(){}
	/**
	 * ��ʼ���׽���,�������������
	 */
	public Server(Socket s){
		try {
			socket = s;
			inStream = socket.getInputStream();// ��ȡ������
			outStream = socket.getOutputStream();// ��ȡ�����
		} catch (Exception e) {
			System.out.println("����˳�ʼ��������������ֹ��ϣ�");
			e.printStackTrace();
		}
	}
	
	/**
	 * ���ڷ���������ָ��ʹ��
	 */
	public void serverSay() {
		new Thread() {
			public void run() {
				try {
					while (true) {
						byte[] outArr = new byte[100];
						System.in.read(outArr);
						outStream.write(outArr);
						System.out.println("������˵:--->>" + new String(outArr).toString());
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
	 * �ڳ������Զ����ͳ�ʼ��ָ��
	 */
	public void serverSayCMD() {
		new Thread() {
			public void run() {
				try {
					//��ʼ����ʱ����ִ��������䣬ʵ�ֲ���
					//����ָ��
					outStream.write("AT+CDAM=1".getBytes());
					outStream.flush();
					System.out.println("������˵:--->>AT+CDAM=1");
					Thread.sleep(500);
					//����ȷ��
					outStream.write("AT+CDAM=1".getBytes());
					outStream.flush();
					System.out.println("������˵:--->>AT+CDAM=1");
					Thread.sleep(500);
					//����ָ��
					outStream.write("AT+CARM=1".getBytes());
					outStream.flush();
					System.out.println("������˵:--->>AT+CARM=1");
					Thread.sleep(500);
					//����ȷ��
					outStream.write("AT+CARM=1".getBytes());
					outStream.flush();
					System.out.println("������˵:--->>AT+CARM=1");
					Thread.sleep(500);
					//�첽ָ��
					outStream.write("AT+CWMSG=SET,0,1".getBytes());
					outStream.flush();
					System.out.println("������˵:--->>AT+CWMSG=SET,0,1");
					
				} catch (Exception e) {
					System.out.println("�ڳ������Զ����ͳ�ʼ��ָ��ʱ���ֹ��ϣ�");
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	/**
	 * ���������ͻ��˷�������Ϣ,����������
	 */
	public void listen() {
		new Thread() {
			public void run() {
				try {
					DataInputStream dis = new DataInputStream(inStream);
					String inMessage = "";// �ͻ��˻ظ�
					String messId = null;//��Ϣid
					String areaId = null;//������
					byte [] buf;//��������
					int len;//��ȡ�ֽ�
					while (true) {
						buf=new byte[1024];
						len=dis.read(buf);
						inMessage = new String(buf,0,len);
						System.out.print("���������ͻ��� ˵:<<---");
						System.out.println(inMessage);
						if (inMessage.indexOf("DFA_ALARM") > -1) {
							dbOperate();
							// ��ȡ��Ϣid
							messId = inMessage.substring(inMessage.indexOf("+CWMSG:") + 7,inMessage.indexOf(','));
							// ��ȡ������
							areaId = inMessage.substring(inMessage.lastIndexOf(',') + 1);
							inMessage = "��" + messId + "����Ϣ,����" + areaId+ ",������";
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					try{
						inStream.close();
						if(socket.isClosed()){
							System.out.println("�ͻ����ж���socket");
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	/**
	 * ���ݿ����(������ѯ�Ƿ���Ҫ����,�Ƿ���δ����ĵǴ���¼,���и��µǴ���¼��������뱨�������,��ʾ��)
	 */
	public void dbOperate() throws Exception{
		try {
			con = DriverManager.getConnection(URL,USERNAME, PASSWORD);
			stmt = con.createStatement();
			// ��ѯ�Ƿ񲼷�״̬ 1:������Ҫ���� 0:��������Ҫ����
			rs = stmt.executeQuery("SELECT t.PARAMVAL FROM SYS_PARAM t WHERE t.PARAMID = 1");
			if (rs.next()) {
				String isDe = rs.getString(1);
				if ("1".equals(isDe)) {
					// ��ѯû�д���ĵǴ���¼
					rs = stmt.executeQuery("SELECT bs.SteamerUpDown_Id as id FROM BUS_STEAMERUpDown bs WHERE bs.Deal_Yn = 0 AND TO_SECONDS(bs.Enter_Tm) > TO_SECONDS(SYSDATE()-100) ORDER BY SteamerUpDown_Id DESC LIMIT 1");
					// �������δ����ĵǴ���¼(����һ�������ڵ�)������³��Ѵ���״̬
					if (rs.next()) {
						playMyMusic(PlayMusic.PASS_MUSIC);//��������һ���̷߳���ͨ����ʾ��
						int id = rs.getInt(1);// ȡ��û�д����һ����¼
						stmt.executeUpdate("UPDATE BUS_STEAMERUPDOWN b set b.Deal_Yn = 1 WHERE b.SteamerUpDown_Id = "+ id);
					} else {
						playMyMusic(PlayMusic.WARN_MUSIC);//��������һ���̷߳���������ʾ��
						rs = stmt.executeQuery("SELECT t.Steamer_Id FROM pty_steamerelecsentry t WHERE t.ElecSentry_Cd = 01");
						if(rs.next()){
							String Id = rs.getString(1);
							// ��������Ѿ�����ĵǴ���¼������뾯����Ϣ
							stmt.executeUpdate("INSERT INTO BUS_STEAMERWARNSTAFF (steamer_id) VALUES('"+Id+"')");
						}
					}
				}
			}
		} catch (SQLException se) {
			System.out.println("���ݿ�����ʧ�ܣ�");
			se.printStackTrace();
		} finally {
			if(rs != null){
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (stmt != null) { // �ر�����
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (con != null) { // �ر����Ӷ���
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * �����������̲߳�����Ƶ
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
		System.out.println("====================�����������ˣ�192.168.1.230����8099�˿�==================");
		ss = new ServerSocket(8099);
		while(true){
			Socket s = ss.accept();
			System.out.println("�пͻ��������ˣ�" + s.getInetAddress() + ":" + s.getPort());
			Server server = new Server(s);
			server.listen();
			server.serverSay();
			server.serverSayCMD();
		}
		
//		long b = System.currentTimeMillis();
//		new Server().dbOperate();
//		System.out.println(System.currentTimeMillis()-b);//�������ݿ��Լ���Ƶ����ʱ��
	}
}