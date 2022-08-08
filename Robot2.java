// ｢海ゲーム｣クライアントプログラムRobot.java
// 競技会用UmiServer3.java 対応版
// このプログラムは,海ゲームのクライアントプログラムです
// 決められた手順で海ゲームをプレイします
// 使い方java Robot 接続先サーバアドレスゲーム参加者名
// 起動後,指定したサーバと接続し,自動的にゲームを行います
// 起動後,指定回数の繰り返しの後,logoutします
// このプログラムはlogoutコマンドがありません
// プログラムを途中で停止するには,以下の手順を踏んでください
// （１）コントロールC を入力してRobotプログラムを停止します
// （２）T1.javaプログラムなど,別のクライアントを使ってRobotと同じ名前でloginします
// （３）logoutします
// 別クライアントからのlogout作業を省略すると,サーバ上に情報が残ってしまいます

// ライブラリの利用
import java.net.*;// ネットワーク関連
import java.io.*;
import java.util.*;

// Robotクラス
public class Robot2 {
	// ロボットの動作タイミングを規定する変数sleeptime
	int sleeptime = 5 ;
	// ロボットがlogoutするまでの時間を規定する変数timeTolive
	int timeTolive = 50 ;
	String line;
	// コンストラクタ
	public Robot2 (String[] args)
	{
		login(args[0],args[1]) ;
		try{
			for(;timeTolive > 0; -- timeTolive){
				System.out.println("あと" + timeTolive + "回") ;
				// 10 回に渡り,sleeptime*100ミリ秒おきにleftコマンドを送ります
				for(int i = 0;i < 10;++i){
					Thread.sleep(sleeptime * 100) ;
					out.println("left");
					out.println("stat");
					out.flush();
					line = in.readLine();
					while (!".".equals(line)) {
						System.out.println(line);
						line = in.readLine();
					}
					line = in.readLine();
					while (!".".equals(line)) {
						System.out.println(line);
						line = in.readLine();
					}
				}

				// 10 回に渡り,sleeptime秒おきにrightコマンドを送ります
				for(int i = 0;i < 10;++i){
					Thread.sleep(sleeptime * 100) ;
					out.println("right");
					out.println("stat");
					out.flush();
					line = in.readLine();
					while (!".".equals(line)) {
						System.out.println(line);
						line = in.readLine();
					}
					line = in.readLine();
					while (!".".equals(line)) {
						System.out.println(line);
						line = in.readLine();
					}
				}
				// upコマンドを1 回送ります
				out.println("up");
				out.println("stat");
				out.flush();
				line = in.readLine();
				while (!".".equals(line)) {
					System.out.println(line);
					line = in.readLine();
				}
				line = in.readLine();
				while (!".".equals(line)) {
					System.out.println(line);
					line = in.readLine();
				}
			}
			// logout処理
			out.println("logout") ;
			out.flush() ;
			server.close() ;
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	// login関連のオブジェクト
	Socket server;// ゲームサーバとの接続ソケット
	int port = 10000;// 接続ポート
	BufferedReader in;// 入力ストリーム
	PrintWriter out;// 出力ストリーム
	String name;// ゲーム参加者の名前

	// loginメソッド
	// サーバへのlogin処理を行います
	void login(String host, String name){
		try {
			// サーバとの接続
			this.name = name;
			server = new Socket(host, port);
			in = new BufferedReader(new InputStreamReader(
			  server.getInputStream()));
			out = new PrintWriter(server.getOutputStream());

			// loginコマンドの送付
			out.println("login " + name);
			out.flush();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	// mainメソッド
	// Robotを起動します
	public static void main(String[] args){
		new Robot2(args);
	}
}