package com.raising.system.server;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class PlayMusic {
	public static String PASS_MUSIC = "pass.wav";
	public static String WARN_MUSIC = "warn.wav";
	public static void main(String[] args) throws Exception{
		play(PASS_MUSIC);//暂时只支持wav格式,并且音频文件需要放在项目根路径下
		Thread.sleep(1000);
		play(WARN_MUSIC);
		Thread.sleep(1000); 
	}
	public static void play(String fileName){
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(
					fileName));// 获得音频输入流
			AudioFormat baseFormat = ais.getFormat();// 指定声音流中特定数据安排
//			System.out.println("baseFormat=" + baseFormat);
			DataLine.Info info = new DataLine.Info(SourceDataLine.class,
					baseFormat);
//			System.out.println("info=" + info);
			SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
			// 从混频器获得源数据行
//			System.out.println("line=" + line);
			line.open(baseFormat);// 打开具有指定格式的行，这样可使行获得所有所需的系统资源并变得可操作。
			line.start();// 允许数据行执行数据 I/O
			int BUFFER_SIZE = 4000 * 4;
			int intBytes = 0;
			byte[] audioData = new byte[BUFFER_SIZE];
			
			while (intBytes != -1) {
				intBytes = ais.read(audioData, 0, BUFFER_SIZE);// 从音频流读取指定的最大数量的数据字节，并将其放入给定的字节数组中。
				if (intBytes >= 0) {
//					int outBytes = 
					line.write(audioData, 0, intBytes);// 通过此源数据行将音频数据写入混频器。
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}