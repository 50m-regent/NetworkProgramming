import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Test {
  private static final int SLEEP_TIME = 600;
  
  private static final int PORT = 10000;
  
  private String line;
  
  private String name;
  
  private Socket server;
  
  private BufferedReader in;
  
  private PrintWriter out;
  
  private Map<String, Coordinate> shipInfo;
  
  private Map<Integer, ArrayList<Coordinate>> energyInfo;
  
  private void login(String paramString1, String paramString2, int paramInt) {
    try {
      this.name = paramString2;
      this.server = new Socket(paramString1, paramInt);
      this
        
        .in = new BufferedReader(new InputStreamReader(this.server.getInputStream()));
      this.out = new PrintWriter(this.server.getOutputStream());
      this.out.println("login " + paramString2);
      this.out.flush();
    } catch (Exception exception) {
      exception.printStackTrace();
      System.exit(1);
    } 
  }
  
  private void logout() throws IOException {
    this.out.println("logout");
    this.out.flush();
    this.server.close();
  }
  
  private final Map<String, Coordinate> readShipInfo() throws IOException {
    HashMap hashMap = new HashMap();
    System.out.println("Ship Info:");
    this.line = this.in.readLine();
    do {
      String[] arrayOfString = this.line.split(" ");
      String str = arrayOfString[0];
      int i = Integer.parseInt(arrayOfString[1]);
      int j = Integer.parseInt(arrayOfString[2]);
      int k = Integer.parseInt(arrayOfString[3]);
      hashMap.put(str, new Coordinate(i, j));
      System.out.println("\t" + str + " (" + k + "): (" + i + ", " + j + ")");
      this.line = this.in.readLine();
    } while (!".".equals(this.line));
    return hashMap;
  }
  
  private final Map<Integer, ArrayList<Coordinate>> readEnergyInfo() throws IOException {
    HashMap hashMap = new HashMap();
    System.out.println("Energy Info:");
    this.line = this.in.readLine();
    if (".".equals(this.line))
      return hashMap; 
    do {
      String[] arrayOfString = this.line.split(" ");
      int i = Integer.parseInt(arrayOfString[0]);
      int j = Integer.parseInt(arrayOfString[1]);
      int k = Integer.parseInt(arrayOfString[2]);
      if (!hashMap.containsKey(Integer.valueOf(k)))
        hashMap.put(Integer.valueOf(k), new ArrayList()); 
      ((ArrayList)hashMap.get(Integer.valueOf(k))).add(new Coordinate(i, j));
      System.out.println("\t(" + i + ", " + j + "): " + k);
      this.line = this.in.readLine();
    } while (!".".equals(this.line));
    return hashMap;
  }
  
  private final String getNeighborShip(Coordinate paramCoordinate) {
    double d = 100000.0D;
    String str = null;
    for (Map.Entry entry : this.shipInfo.entrySet()) {
      Coordinate coordinate = (Coordinate)entry.getValue();
      double d1 = Coordinate.distance(paramCoordinate, coordinate);
      if (d1 < d) {
        str = (String)entry.getKey();
        d = d1;
      } 
    } 
    return str;
  }
  
  private final Coordinate getTarget() {
    Coordinate coordinate = new Coordinate(0, 0);
    double d = 100000.0D;
    for (Map.Entry entry : this.energyInfo.entrySet()) {
      for (Coordinate coordinate1 : (ArrayList)entry.getValue()) {
        double d1 = Coordinate.distance(coordinate1, (Coordinate)this.shipInfo.get(this.name));
        if (this.name.equals(getNeighborShip(coordinate1)) && d1 < d) {
          coordinate = coordinate1;
          d = d1;
        } 
      } 
    } 
    System.out.println("Target: (" + coordinate.getX() + ", " + coordinate.getY() + ")");
    return coordinate;
  }
  
  private final String calculateMove() {
    Coordinate coordinate1 = getTarget();
    Coordinate coordinate2 = (Coordinate)this.shipInfo.get(this.name);
    int i = coordinate1.getX();
    int j = coordinate1.getY();
    int k = coordinate2.getX();
    int m = coordinate2.getY();
    if (Math.abs(i - k) >= 8) {
      int n = (i - k + 256) % 256;
      int i1 = (k - i + 256) % 256;
      if (n < i1)
        return "right"; 
      return "left";
    } 
    if (Math.abs(j - m) >= 8) {
      int n = (j - m + 256) % 256;
      int i1 = (m - j + 256) % 256;
      if (n < i1)
        return "up"; 
      return "down";
    } 
    return "up";
  }
  
  private void makeMove(String paramString) {
    switch (paramString) {
      case "left":
      case "up":
      case "right":
      case "down":
        this.out.println(paramString);
        this.out.flush();
        this.shipInfo.replace(this.name, ((Coordinate)this.shipInfo.get(this.name)).add((Coordinate)Game.MOVE_UNIT.get(paramString)));
        break;
    } 
  }
  
  private void readStat() throws IOException {
    this.out.println("stat");
    this.out.flush();
    this.line = this.in.readLine();
    this.shipInfo = readShipInfo();
    this.line = this.in.readLine();
    this.energyInfo = readEnergyInfo();
  }
  
  private void progress() throws IOException {
    readStat();
    for (byte b = 0; b < 2; b++) {
      String str = calculateMove();
      makeMove(str);
      System.out.println("Sent command: " + str);
    } 
    System.out.println("");
  }
  
  public Test(String paramString1, String paramString2, int paramInt) {
    boolean bool = false;
    login(paramString1, paramString2, paramInt);
    try {
      do {
        progress();
        Thread.sleep(600L);
      } while (!bool);
      logout();
    } catch (Exception exception) {
      exception.printStackTrace();
      System.exit(1);
    } 
  }
  
  public static void main(String[] paramArrayOfString) {
    int i = 10000;
    switch (paramArrayOfString.length) {
      case 3:
        i = Integer.parseInt(paramArrayOfString[2]);
        break;
      case 2:
        break;
      default:
        System.err.println("usage: java Robot3 host name [port]");
        System.exit(1);
        break;
    } 
    String str1 = paramArrayOfString[0];
    String str2 = paramArrayOfString[1];
    new Test(str1, str2, i);
  }
}
