import java.net.*;
import java.io.*;
import java.util.*;

class Game {
    public static final int FIELD_WIDTH  = 256;
    public static final int FIELD_HEIGHT = 256;
    public static final int MAAI         = 8;

    private static final int STRIDE = 10;
    public static final Map<String, Coordinate> MOVE_UNIT = new HashMap<>() {
        {
            put("left",  new Coordinate(-STRIDE, 0));
            put("up",    new Coordinate(0,       STRIDE));
            put("right", new Coordinate(STRIDE,  0));
            put("down",  new Coordinate(0,       -STRIDE));
        }
    };
}

class Coordinate extends AbstractMap.SimpleEntry<Integer, Integer> {
    public Coordinate(final int x, final int y) {
        super(x, y);
    }

    public final int getX() {
        return getKey();
    }

    public final int getY() {
        return getValue();
    }

    public final Coordinate add(final Coordinate coordinate) {
        return new Coordinate(getX() + coordinate.getX(), getY() + coordinate.getY());
    }

    public static final double distance(final Coordinate coordinate1, final Coordinate coordinate2) {
        final int x1 = coordinate1.getX();
        final int y1 = coordinate1.getY();
        final int x2 = coordinate2.getX();
        final int y2 = coordinate2.getY();

        final int xDistance = Math.min(
            (x1 - x2 + Game.FIELD_WIDTH) % Game.FIELD_WIDTH,
            (x2 - x1 + Game.FIELD_WIDTH) % Game.FIELD_WIDTH
        );
        final int yDistance = Math.min(
            (y1 - y2 + Game.FIELD_HEIGHT) % Game.FIELD_HEIGHT,
            (y2 - y1 + Game.FIELD_HEIGHT) % Game.FIELD_HEIGHT
        );
        return xDistance + yDistance;
    }
}

public class Robot3 {
	final private static int SLEEP_TIME = 600;
    final private static int PORT       = 10000;

	private String line;
    private String name;
    private Socket server;
	private BufferedReader in;
	private PrintWriter out;

    private Map<String, Coordinate> shipInfo;
    private Map<Integer, ArrayList<Coordinate>> energyInfo;

	private void login(final String host, final String name, final int port) {
		try {
			this.name = name;
			server    = new Socket(host, port);

			in  = new BufferedReader(
                new InputStreamReader(
			        server.getInputStream()
                )
            );
			out = new PrintWriter(server.getOutputStream());

			out.println("login " + name);
			out.flush();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

    private void logout() throws IOException {
        out.println("logout");
        out.flush();
        server.close();
    }

    final private Map<String, Coordinate> readShipInfo() throws IOException {
        final Map<String, Coordinate> shipInfo = new HashMap<>();

        System.out.println("Ship Info:");

        line = in.readLine();
        do {
            final String[] lineData = line.split(" ");

            final String shipName = lineData[0];

            final int x     = Integer.parseInt(lineData[1]);
            final int y     = Integer.parseInt(lineData[2]);
            final int point = Integer.parseInt(lineData[3]);

            shipInfo.put(shipName, new Coordinate(x, y));

            System.out.println("\t" + shipName + " (" + point + "): (" + x + ", " + y + ")");

            line = in.readLine();
        } while (!".".equals(line));

        return shipInfo;
    }

    final private Map<Integer, ArrayList<Coordinate>> readEnergyInfo() throws IOException {
        final Map<Integer, ArrayList<Coordinate>> energyInfo = new HashMap<>();

        System.out.println("Energy Info:");

        line = in.readLine();
        if (".".equals(line)) {
            return energyInfo;
        }

        do {
            final String[] lineData = line.split(" ");

            final int x     = Integer.parseInt(lineData[0]);
            final int y     = Integer.parseInt(lineData[1]);
            final int point = Integer.parseInt(lineData[2]);

            if (!energyInfo.containsKey(point)) {
                energyInfo.put(point, new ArrayList<Coordinate>());
            }

            energyInfo.get(point).add(new Coordinate(x, y));

            System.out.println("\t(" + x + ", " + y + "): " + point);

            line = in.readLine();
        } while (!".".equals(line));

        return energyInfo;
    }

    final private String getNeighborShip(final Coordinate coordinate) {
        double minDistance = 100000;
        String neighbor = null;

        for (final Map.Entry<String, Coordinate> entry: shipInfo.entrySet()) {
            final Coordinate shipCoordinate = entry.getValue();
            final double distance = Coordinate.distance(coordinate, shipCoordinate);

            if (distance < minDistance) {
                neighbor = entry.getKey();
                minDistance = distance;
            }
        }

        return neighbor;
    }

    final private Coordinate getTarget() {
        Coordinate target = new Coordinate(0, 0);
        double minDistance = 100000;

        for (final Map.Entry<Integer, ArrayList<Coordinate>> entry: energyInfo.entrySet()) {
            for (final Coordinate coordinate: entry.getValue()) {
                final double distance = Coordinate.distance(coordinate, shipInfo.get(name));
                if (name.equals(getNeighborShip(coordinate)) && distance < minDistance) {
                    target = coordinate;
                    minDistance = distance;
                }
            }
        }

        System.out.println("Target: (" + target.getX() + ", " + target.getY() + ")");
        
        return target;
    }

    final private String calculateMove() {
        final Coordinate target  = getTarget();
        final Coordinate selfPos = shipInfo.get(name);

        final int targetX = target.getX();
        final int targetY = target.getY();
        final int selfX   = selfPos.getX();
        final int selfY   = selfPos.getY();

        if (Math.abs(targetX - selfX) >= Game.MAAI) {
            final int rightDistance = (targetX - selfX + Game.FIELD_WIDTH) % Game.FIELD_WIDTH;
            final int leftDistance  = (selfX - targetX + Game.FIELD_WIDTH) % Game.FIELD_WIDTH;

            if (rightDistance < leftDistance) {
                return "right";
            }
            return "left";
        } else if (Math.abs(targetY - selfY) >= Game.MAAI) {
            final int upDistance   = (targetY - selfY + Game.FIELD_HEIGHT) % Game.FIELD_HEIGHT;
            final int downDistance = (selfY - targetY + Game.FIELD_HEIGHT) % Game.FIELD_HEIGHT;

            if (upDistance < downDistance) {
                return "up";
            }
            return "down";
        }

        return "up";
    }

    private void makeMove(final String move) {
        switch (move) {
            case "left":
            case "up":
            case "right":
            case "down":
                out.println(move);
                out.flush();

                shipInfo.replace(name, shipInfo.get(name).add(Game.MOVE_UNIT.get(move)));
                break;
            default:
                break;
        }
    }

    private void readStat() throws IOException {
        out.println("stat");
        out.flush();

        line = in.readLine();
        shipInfo = readShipInfo();

        line = in.readLine();
        energyInfo = readEnergyInfo();
    }

    private void progress() throws IOException {
        String move;

        readStat();

        for (int i = 0; i < 2; i++) {
            move = calculateMove();
            makeMove(move);
            System.out.println("Sent command: " + move);
        }

        System.out.println("");
    }

	public Robot3(final String host, final String name, final int port) {
        boolean isEnd = false;

		login(host, name, port);

		try {
			while (true) {
				progress();
                Thread.sleep(SLEEP_TIME);

                if (isEnd) break;
			}

			logout();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args){
        int port = PORT;

        switch (args.length) {
            case 3:
                port = Integer.parseInt(args[2]);
            case 2:
                break;
            default:
                System.err.println("usage: java Robot3 host name [port]");
                System.exit(1);
                break;
        }

        final String host = args[0];
        final String name = args[1];

		new Robot3(host, name, port);
	}
}