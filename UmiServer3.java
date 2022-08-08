// ｢海ゲーム｣サーバプログラムUmiServer3.java
// このプログラムは,海ゲームのサーバプログラムです。
//
// Todo:
// ゲームスタート時に"Game Start!"と2秒間画面に表示するようにする
// テキストのプログラムを拡張し、
// エネルギータンクが数種類ある（点数が異なる）形に変更。完了。
// （1点から4点で出現頻度を 25%、25%、25%、25% とする）
//
// 使い方>java UmiServer3 参加人数 燃料間隔[ms](def=5000) プレイ時間[s](def=180)
//
// 起動すると,ポート番号10000 番に対するクライアントからの接続を待ちます
// プログラムを停止するにはコントロールC を入力してください

// ライブラリの利用
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;// グラフィックス

// UmiServerクラス
// UmiServer3クラスは,UmiServerプログラムの本体です
public class UmiServer3 {
	static final int DEFAULT_PORT = 10000;
							//UmiServer接続用ポート番号
	static ServerSocket serverSocket;
	static Vector<Socket> connections;
				//クライアントとのコネクションを保持するVectorオブジェクト
	static Vector<int[]> energy_v; // 燃料タンクの位置情報リスト
	static Hashtable<String, Ship> userTable = null;
				// クライアント関連情報登録用テーブル
	static Random random = null;
				// 燃料タンクを追加するスレッド
	static Thread et;

	static Thread pt; // 定期的再描画タスク
				// 時間を経過させるタイマー＆タイマータスク
	static Timer timer;
	static TimerTask ttask;
				// 参加する人数
	static int player_num;
				// 競技時間[s]
	static int play_time = 180;
				// 燃料間隔[ms]
	static int energy_interval = 5000;
	static int num = 0;	// 現在参加している人数
				// ゲーム終了のブール値
	static boolean is_finish = false;
	static int[] scores;
	static String[] names;
	
	// 画面表示用インスタンス
	static Frame f;// クライアント情報表示用ウィンドウ
	static Panel p;// 上下左右の移動ボタンと海の状態を表示するパネル
	static Canvas c;// 海の状態を表示するキャンバス

	static Image    imb; // ダブルバッファリング用
	static Graphics g;  // ダブルバッファリング用
	static Graphics fg; // foreground

	// addConnectionメソッド
	// クライアントとの接続をVectorオブジェクトconnectionsに登録します
	public static void addConnection(Socket s){
		if (connections == null){//初めてのコネクションの場合は,
			connections = new Vector<Socket>();// connectionsを作成します
		}
		connections.addElement(s);
	}

	// deleteConnectionメソッド
	// クライアントとの接続をconnectionsから削除します
	public static void deleteConnection(Socket s){
		if (connections != null){
			connections.removeElement(s);
		}
	}

	// loginUserメソッド
	// loginコマンドの処理として,利用者の名前や船の位置を登録します
	public static int loginUser(String name){
		if (userTable == null){// 登録用テーブルがなければ作成します
			userTable = new Hashtable<String, Ship>();
		}
		if (random == null){// 乱数の準備をします
			random = new Random();
		}
		
		// すでに参加人数満たしていたら拒否
		if( num == player_num ){
			System.out.println("invalid login: No more player can login.");
			System.out.flush();
			return 0;
		}
		
		// すでに参加している名前は拒否
		if( userTable.containsKey(name) ){
			System.out.println("invalid login: "+name+" has already logined.");
			System.out.flush();
			return 0;
		}
		
		// 船の初期位置を乱数で決定します
		int ix = Math.abs(random.nextInt()) % 256;
		int iy = Math.abs(random.nextInt()) % 256;

		// クライアントの名前や船の位置を表に登録します
		userTable.put(name, new Ship(ix, iy));
		// サーバ側の画面にもクライアントの名前を表示します
		//System.out.println(""+num);
		System.out.println("login:" + name);
		System.out.flush();
		
		num++;
		
		// 参加人数に達したら開始
		if( num == player_num ){
			et.start();
			timer = new Timer();
			timer.schedule(ttask, 1000, 1000);
			System.out.println("Game start!!");
			System.out.flush();
		}
		return 1;
	}

	// logoutUserメソッド
	// クライアントのログアウトを処理します
	public static void logoutUser(String name){
		// サーバ側画面にログアウトするクライアントの名前を表示します
		System.out.println("logout:" + name);
		System.out.flush();
		// 登録用テーブルから項目を削除します
		userTable.remove(name);
	}

	// leftメソッド
	// ある特定の船を左に動かして,燃料タンクが拾えるかどうか判定します
	// 判定にはcalculationメソッドを使います
	public static void left(String name){
		Ship ship = (Ship) userTable.get(name);
		ship.left();
		calculation();
	}

	// rightメソッド
	// ある特定の船を右に動かして,燃料タンクが拾えるかどうか判定します
	// 判定にはcalculationメソッドを使います
	public static void right(String name){
		Ship ship = (Ship) userTable.get(name);
		ship.right();
		calculation();
	}

	// upメソッド
	// ある特定の船を上に動かして,燃料タンクが拾えるかどうか判定します
	// 判定にはcalculationメソッドを使います
	public static void up(String name){
		Ship ship = (Ship) userTable.get(name);
		ship.up();
		calculation();
	}

	// downメソッド
	// ある特定の船を下に動かして,燃料タンクが拾えるかどうか判定します
	// 判定にはcalculationメソッドを使います
	public static void down(String name){
		Ship ship = (Ship) userTable.get(name);
		ship.down();
		calculation();
	}

	// calculationメソッド
	// 燃料タンクと船の位置関係を調べて,燃料タンクが拾えるかどうか判定します
	static void calculation(){
		if (userTable != null && energy_v != null){
			// すべてのクライアントについて判定します
			for (Enumeration users = userTable.keys();
				 users.hasMoreElements();) {
				// 判定するクライアントの名前と船の位置を取り出します
				String user = users.nextElement().toString();
				Ship ship = (Ship) userTable.get(user);
				// 燃料タンクすべてについて,船との位置関係を調べます
				for (Enumeration energys = energy_v.elements();
					 energys.hasMoreElements();) {
					// 燃料タンクの位置と船の位置を調べ,距離を計算します
					try {
						if (energys.hasMoreElements()) {
							int[] e = (int []) energys.nextElement();
							int x = e[0] - ship.x;
							int y = e[1] - ship.y;
							// 海の淵での距離計算を正しくする
							if (x >  128) x -= 256;
							if (x < -128) x += 256;
							if (y >  128) y -= 256;
							if (y < -128) y += 256;
							// double r = Math.sqrt(x * x + y * y);
							// 距離"10"より近いなら燃料タンクを取り込みます
							if (x*x+y*y < 100){ // if (r < 10)
								ship.point += e[2]; 
								energy_v.removeElement(e);
							}
						}
					} catch (Exception err) {
						System.out.println("error in calculation:" + err);
					}
				}
			}
		}
	}

	// statInfoメソッド
	// STATコマンドを処理します
	// クライアントに対して,船の情報(ship_info)と,
	// 海上を漂流している燃料タンクの情報を(energy_info)を送信します
	public static void statInfo(PrintWriter pw){
		// 船の情報(ship_info)の送信
		pw.println("ship_info");
		if (userTable != null){
			for (Enumeration users = userTable.keys();
				 users.hasMoreElements();) {
				String user = users.nextElement().toString();
				Ship ship = (Ship) userTable.get(user);
				pw.println(user + " " + ship.x + " "
								+ ship.y + " " + ship.point);
			}
		}
		pw.println(".");// ship_infoの終了
		// 燃料タンクの情報（energy_info）の送信
		pw.println("energy_info");
		if (energy_v != null){
			// すべての燃料タンクの位置情報をクライアントに送信します
			try {
				for (Enumeration energys = energy_v.elements();
					 energys.hasMoreElements();) {
					int[] e = (int []) energys.nextElement();
					pw.println(e[0] + " " + e[1] + " " + e[2]);
				//	System.out.println(e[0] + " " + e[1] + " " + e[2]);
				}
			} catch (Exception err){
				System.out.println("statInfo error: "+err);
			}
		}
		pw.println(".");// enegy_infoの終了
		pw.flush();
	}
	
	// statInfo_dummyメソッド
	// 違反のSTATコマンドを処理します
	// 情報0です
	public static void statInfo_dummy(PrintWriter pw){
		pw.println("ship_info");
		pw.println(".");
		pw.println("energy_info");
		pw.println(".");
		pw.flush();
	}

	// putEnergyメソッド
	// 燃料タンクを１つだけ,海上にランダムに配置します
	public static void putEnergy(){
		if (energy_v == null){// 初めて配置する場合の処理
			energy_v = new Vector<int[]>();
		}
		if (random == null){// 初めて乱数を使う場合の処理
			random = new Random();
		}
		// 乱数で位置を決めて海上に配置します
		int[] e = new int[3];
		e[0] = Math.abs(random.nextInt()) % 256;
		e[1] = Math.abs(random.nextInt()) % 256;
		int tmpval = Math.abs(random.nextInt()) % 8;
		// エネルギータンクの点数 1から4点。1点25%、2点25%、3点25%、4点25%
		if      (tmpval < 2) e[2] = 1;
		else if (tmpval < 4) e[2] = 2;
		else if (tmpval < 6) e[2] = 3;
		else                 e[2] = 4; // エネルギータンクの点数
		energy_v.addElement(e);
	}
	
	// finishメソッド
	public static void finish(){
		int i,j;
		scores = new int[player_num];
		names = new String[player_num];
		
		// 勝者判定
		i = 0;
		for( Enumeration e = userTable.keys(); e.hasMoreElements(); i++){
			String name = e.nextElement().toString();
			Ship ship = (Ship)userTable.get(name);
			
			scores[i] = ship.point;
			names[i] = name;
		}
		
		int tmp;
		String tmp2;
		
		// ソート
		for(i=0; i<player_num-1; i++){
			for(j=i+1; j<player_num; j++){
				if( scores[j] > scores[i] ){
					tmp = scores[i];	scores[i] = scores[j];	scores[j] = tmp;
					tmp2 = names[i];	names[i] = names[j];	names[j] = tmp2;
				}
			}
		}
		
		for(i=0; i<player_num; i++){
			System.out.println(""+(i+1)+"位："+names[i]+", "+scores[i]+"点");
		}
		
		is_finish = true;
		
		paint();
	}
	
	public static void repaint() {
		fg.drawImage(imb,0,0,null);
	}

	// paintメソッド
	// 現在の状況を表示します
	public static void paint()
	{
		int x,y;
//		Graphics g = c.getGraphics();
		Enumeration e;
		String name;
		
		// フォントサイズを2倍に
		Font font     = new Font(null, Font.PLAIN, 20);
		Font fontmini = new Font(null, Font.PLAIN, 16);
		g.setFont(font);
		
		// 海の描画(単なる青い四角形です)
		g.setColor(Color.blue);
//		g.fillRect(0, 20, 512, 512);
		g.fillRect(0, 20, 532, 580);

		// 燃料の表示
		if( energy_v != null ){
			try {
				for( e = energy_v.elements(); e.hasMoreElements(); ){
					int[] p = (int [])e.nextElement();
					x = p[0]*2;
					y = p[1]*2;
					
					// 燃料タンクは,白抜きの赤丸で示します
					g.setColor(Color.red);
					g.fillOval(x - 10, 532 - y - 10, 20, 20);
					g.setColor(Color.white);
					g.fillOval(x - 6, 532 - y - 6, 12, 12);
					g.setColor(Color.black);
					g.setFont(fontmini);
					g.drawString(""+p[2], x-6+2, 532-y+6);
				}
			} catch (Exception err) {
				System.out.println("error in paint:" + err);
			}
		}
		
		// 船の描画
		if( userTable != null ){
			g.setFont(font);
			for( e = userTable.keys(); e.hasMoreElements(); ){
				name = e.nextElement().toString();
				Ship ship = (Ship) userTable.get(name);
				x = ship.x*2;
				y = ship.y*2;
				
				// 影つきにする（黒で一回描画）
				// 船を表示します
				g.setColor(Color.black);
				g.fillOval(x - 19, 532 - y - 19, 40, 40);
				if (x < 40)     g.fillOval(532+x-19,532-y-19,40,40);
				if (x > 532-40) g.fillOval(-532+x-19,532-y-19,40,40);
				if (y < 40)     g.fillOval(x-19,-y-19,40,40);
				if (y > 532-40) g.fillOval(x-19,1064-y-19,40,40);
				// 得点を船の右下に表示します
				g.drawString(""+ship.point, x + 21, 532 - y + 19) ;
				if (x > 532-60) g.drawString(""+ship.point,-532+x+21, 532-y+19);
				if (y < 40) g.drawString(""+ship.point,x+21, -y+19);
				if (x>532-60&&y<40) g.drawString(""+ship.point,-532+x+21,-y+19);
				// 名前を船の右上に表示します
				g.drawString(name, x+21, 532-y-19) ;
				if (x > 532-80) g.drawString(name, -532+x+21, 532-y-19);
				if (y > 532-40) g.drawString(name, x+21, 1064-y-19);
				if (x>532-80&&y>532-40) g.drawString(name, -532+x+21, 1064-y-19);

				// 船を表示します
				g.setColor(Color.green);
				g.fillOval(x - 20, 532 - y - 20, 40, 40);
				if (x < 40)     g.fillOval(532+x-20,532-y-20,40,40);
				if (x > 532-40) g.fillOval(-532+x-20,532-y-20,40,40);
				if (y < 40)     g.fillOval(x-20,-y-20,40,40);
				if (y > 532-40) g.fillOval(x-20,1064-y-20,40,40);
				// 得点を船の右下に表示します
				g.drawString(""+ship.point, x + 20, 532 - y + 20) ;
				if (x > 532-60) g.drawString(""+ship.point,-532+x+20, 532-y+20);
				if (y < 40) g.drawString(""+ship.point,x+20, -y+20);
				if (x>532-60&&y<40) g.drawString(""+ship.point,-532+x+20,-y+20);
				// 名前を船の右上に表示します
				g.drawString(name, x+20, 532-y-20) ;
				if (x > 532-80) g.drawString(name, -532+x+20, 532-y-20);
				if (y > 532-40) g.drawString(name, x+20, 1064-y-20);
				if (x>532-80&&y>532-40) g.drawString(name, -532+x+20, 1064-y-20);
			}
		}
			
		// 情報表示エリアの描画
		g.setColor(Color.black);
		g.fillRect(0, 0, 532, 20);
		
		// 情報の描画
		g.setColor(Color.white);
		g.drawString("残り時間："+play_time, 0, 20);

		// 誰もログインしていないときは、開始前画面
		if( num == 0 ) {
			String str = "Umi Game";
			font = new Font(null, Font.PLAIN, 60);
			g.setFont(font);
			g.setColor(Color.green);
			int width = g.getFontMetrics().stringWidth(str);
			g.drawString(str, 256-width/2, 256-15);
		}
		
		// 勝者の表示
		if( is_finish && scores.length > 1){
			String str;
			
			if( scores[0] == scores[1] ){
				str = "DRAW !!";
			} else {
				str = names[0] + " WIN !!";
			}
			font = new Font(null, Font.PLAIN, 60);
			g.setFont(font);
			g.setColor(Color.red);
			int width = g.getFontMetrics().stringWidth(str);
			g.drawString(str, 256-width/2, 256-15);
		}

		fg.drawImage(imb,0,0,null);
	}

	// mainメソッド
	// サーバソケットの作成とクライアント接続の処理
	// および適当なタイミングでの燃料タンクの逐次追加処理を行います
	public static void main(String[] args){
		try {// サーバソケットの作成
			serverSocket = new ServerSocket(DEFAULT_PORT);
		}catch (IOException e){
			System.err.println("can't create server socket.");
			System.exit(1);
		}
		
		// 引数処理
		switch( args.length ){
			case 3:	play_time = Integer.parseInt(args[2]);
			case 2:	energy_interval = Integer.parseInt(args[1]);
			case 1:	player_num = Integer.parseInt(args[0]);
				break;
			default:
				System.err.println("usage:\njava UmiServer player_num [energy_interval(ms)] [play_time(s)]");
				System.exit(1);
				break;
		}


		pt = new Thread() { // 定期的に再描画するタスクを作ります。
			public void run() {
				while(true){
					try {
						sleep(10);
					} catch (InterruptedException e) {
						break;
					}
					UmiServer3.paint();

//					if (is_finish) {
//						break;
//					}
				}
			}
		};

		
		// 燃料タンクを順に追加するスレッドetを作ります
		et = new Thread(){
			public void run(){
				while(true){
					try {
						// スレッドetを休止させます
						sleep(UmiServer3.energy_interval);
					}catch(InterruptedException e){
						break;
					}
					// 海上に１つ燃料タンクを配置します
					if( UmiServer3.play_time > 0 ){
						UmiServer3.putEnergy();
//						UmiServer3.paint();
					} else
						break;
				}
			}
		};
		
		// 時間を経過させるタイマータスクttaskを作ります
		ttask = new TimerTask(){
			public void run(){
				UmiServer3.play_time--;
//				UmiServer3.paint();
				
				// 終了処理
				if( UmiServer3.play_time == 0 ){
					UmiServer3.timer.cancel();
					UmiServer3.finish();
				}
			}
		};
		
		f = new Frame();//クライアント情報ウィンドウ全体の表示
		p = new Panel();//海表示部分と操作ボタンの表示
		p.setLayout(new BorderLayout());
		
		// 海上の様子を表示するCanvasを作成します
		c = new Canvas();
		c.setSize(532, 600);	// 大きさの設定
		// フレームに必要な部品を取り付けます
		p.add(c);
		f.add(p);

		// フレームfを表示します
		f.setSize(532, 600);
		f.setVisible(true);

		imb = p.createImage(532,600); 
		g = imb.getGraphics();
		g.setColor(Color.blue);
		g.fillRect(0, 20, 532, 580);
		g.setColor(Color.black);
		g.fillRect(0, 0, 532, 20);

		fg = c.getGraphics();
		fg.drawImage(imb,0,0,null);
		
//		paint();
		pt.start(); // 定期描画タスク
		
		// ソケットの受付と,クライアント処理プログラムの開始処理を行います
		while (true) {// 無限ループ
			try {
				Socket cs = serverSocket.accept();
				addConnection(cs);// コネクションを登録します
				// クライアント処理スレッドを作成します
				Thread ct = new Thread(new clientProc(cs));
				ct.start();
			}catch (IOException e){
				System.err.println("client socket or accept error.");
			}
		}
	}
}

// clientProcクラス
// clientProcクラスは,クライアント処理スレッドのひな形です
class clientProc implements Runnable {
	Socket s; // クライアント接続用ソケット
	// 入出力ストリーム
	BufferedReader in;
	PrintWriter out;
	String name = null;// クライアントの名前
	Date date;
	long	lasttime = 0;	// 最後にstatコマンドを送った時刻
	int		command=2;		// コマンド回数

	// コンストラクタclientProc
	// ソケットを使って入出力ストリームを作成します
	public clientProc(Socket s) throws IOException {
		this.s = s;
		in = new BufferedReader(new InputStreamReader(
					s.getInputStream()),8192);
		out = new PrintWriter(s.getOutputStream());
	}

	// runメソッド
	// クライアント処理スレッドの本体です
	public void run(){
		try {
			//LOGOUTコマンド受信かゲーム終了まで繰り返します
			while ( true ) {
				// クライアントからの入力を読み取ります
				String line = in.readLine();
				if (line == null) {
					System.out.println("null");
					continue;
				}
// if (name != null) System.out.println(name+"["+line+"]");

				// nameが空の場合にはLOGINコマンドのみを受け付けます
				if (name == null){
					StringTokenizer st = new StringTokenizer(line);
					String cmd = st.nextToken();
					if ("login".equalsIgnoreCase(cmd)){
						name = st.nextToken();
						if (UmiServer3.loginUser(name) == 0) break;
						// ログインできなかったらクライアントとの接続終了
					}else{
						// LOGINコマンド以外は,すべて無視します
						continue;
					}
					
				// ゲーム終了したらLOGOUTコマンドのみを受け付けます
				}else if( UmiServer3.is_finish ){
					StringTokenizer st = new StringTokenizer(line);
					String cmd = st.nextToken();
					if( cmd.equalsIgnoreCase("LOGOUT") ){
						UmiServer3.logoutUser(name);
						break;
					}
				}else{
					// nameが空でない場合はログイン済みですから,コマンドを受け付けます
					StringTokenizer st = new StringTokenizer(line);
					String cmd = st.nextToken();// コマンドの取り出し
					// コマンドを調べ,対応する処理を行います
					if ("STAT".equalsIgnoreCase(cmd)){
						// 500ミリ秒経っていれば受け付ける
						date = new Date();
						if( date.getTime() - lasttime >= 500 ){
							UmiServer3.statInfo(out);
							lasttime = date.getTime();	// 時刻取得
							command = 2;
						} else {
							UmiServer3.statInfo_dummy(out);
						}
					} else if ("LOGOUT".equalsIgnoreCase(cmd)){
						UmiServer3.logoutUser(name);
						// LOGOUTコマンドの場合には繰り返しを終了します
						break;
					} else {
						// コマンド回数制限
						if( command > 0 ){
							if ("UP".equalsIgnoreCase(cmd)){
								UmiServer3.up(name);
							} else if ("DOWN".equalsIgnoreCase(cmd)){
								UmiServer3.down(name);
							} else if ("LEFT".equalsIgnoreCase(cmd)){
								UmiServer3.left(name);
							} else if ("RIGHT".equalsIgnoreCase(cmd)){
								UmiServer3.right(name);
							} else {
								continue;
							}
							command--;
						} else {
							continue;
						}
					}
				}
				
//				UmiServer3.paint();
			}
			// 登録情報を削除し,接続を切断します
			UmiServer3.deleteConnection(s);
			s.close();
		}catch (IOException e){
			try {
				s.close();
				if (name != null ) System.out.println(name+" "+e+"(IOEXCEPTION)");
			}catch (IOException e2){
				if (name != null ) System.out.println(name+" "+e2+"(IOEXCEPTION) e2");
			}
		}
	}
}

// Shipクラス
// 船の位置と,獲得した燃料タンクの数を管理します
class Ship {
	// 船の位置座標
	int x;
	int y;
	// 獲得した燃料タンクの個数
	int point = 0;

	// コンストラクタ
	// 初期位置をセットします
	public Ship(int x, int y){
		this.x = x;
		this.y = y;
	}

	// leftメソッド
	// 船を左に動かします
	public void left(){
		x -= 10;
		// 左の辺は右の辺につながっています
		if (x < 0)
			x += 256;
	}

	// rightメソッド
	// 船を右に動かします
	public void right(){
		x += 10;
		// 右の辺は左の辺につながっています
		x %= 256;
	}

	// upメソッド
	// 船を上に動かします
	public void up(){
		y += 10;
		// 上の辺は下の辺につながっています
		y %= 256;
	}

	// downメソッド
	// 船を下に動かします
	public void down(){
		y -= 10;
		// 下の辺は上の辺につながっています
		if (y < 0)
			y += 256;
	}
}