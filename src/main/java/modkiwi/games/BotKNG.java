package modkiwi.games;

import modkiwi.data.GameInfo;
import modkiwi.util.Logger;
import modkiwi.util.Utils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BotKNG extends GameBot {
    public static final String LONG_NAME = "Kingdoms";

    private static final Logger LOGGER = new Logger(BotKNG.class);

    private static final Map<String, String> IMAGES = createImageMap();
    private static final String[] ALL_COLORS = new String[] {"blue", "green", "orange", "purple", "red", "yellow"};

    private String[][] board;
    private int[][] castles;
    private String[] hand, colors;
    private int[] scores;
    private Map<String, Integer> revColors;
    private String step;
    private int turn;
    private int round;
    private List<String> deck;

    private static Map<String, String> createImageMap() {
        Map<String, String> images = new HashMap<String, String>();
        images.put("blue1",   "2635465");
        images.put("blue2",   "2635466");
        images.put("blue3",   "2635467");
        images.put("blue4",   "2635468");
        images.put("green1",  "2635469");
        images.put("green2",  "2635470");
        images.put("green3",  "2635471");
        images.put("green4",  "2635472");
        images.put("orange1", "2635473");
        images.put("orange2", "2635474");
        images.put("orange3", "2635475");
        images.put("orange4", "2635476");
        images.put("purple1", "2635477");
        images.put("purple2", "2635478");
        images.put("purple3", "2635479");
        images.put("purple4", "2635480");
        images.put("blue1",   "2635481");
        images.put("blue2",   "2635482");
        images.put("blue3",   "2635483");
        images.put("blue4",   "2635484");
        images.put("yellow1", "2635485");
        images.put("yellow2", "2635486");
        images.put("yellow3", "2635487");
        images.put("yellow4", "2635488");
        images.put("empty",   "2635489");
        images.put("rowA",    "2635490");
        images.put("rowB",    "2635491");
        images.put("rowC",    "2635492");
        images.put("rowD",    "2635493");
        images.put("rowE",    "2635494");
        images.put("header",  "2635558");
        images.put("-1",      "2635496");
        images.put("-2",      "2635497");
        images.put("-3",      "2635498");
        images.put("-4",      "2635499");
        images.put("-5",      "2635500");
        images.put("-6",      "2635501");
        images.put("+1",      "2635502");
        images.put("+2",      "2635503");
        images.put("+3",      "2635504");
        images.put("+4",      "2635505");
        images.put("+5",      "2635506");
        images.put("+6",      "2635507");
        images.put("mountain","2635508");
        images.put("goldmine","2635509");
        images.put("dragon",  "2635510");
        images.put("wizard",  "2635511");
        return images;
    }

    protected BotKNG(GameInfo game) throws IOException {
        super(game);
    }

    private static String getImage(String key, String... args) {
        String image = "[imageid=" + IMAGES.get(key);
        for (String s : args)
            image += " " + s;
        image += "]";
        return image;
    }

    @Override
    public void createGame() {
        deck = new ArrayList<String>(22);
        for (int i = 1; i <= 6; i++) {
            deck.add("+" + i);
            deck.add("+" + i);
            deck.add("-" + i);
        }
        deck.add("dragon");
        deck.add("goldmine");
        deck.add("mountain");
        deck.add("mountain");

        Collections.shuffle(deck);
        game.getData().setProperty("round1deck", deck);

        deck = new ArrayList<String>(deck);
        Collections.shuffle(deck);
        game.getData().setProperty("round2deck", deck);

        deck = new ArrayList<String>(deck);
        Collections.shuffle(deck);
        game.getData().setProperty("round3deck", deck);
    }

    private void mailTiles() {
        for (int i = 0; i < NoP; i++) {
            String message = String.format("[color=#008800]Your tile for round %d is %s.[/color]\n%s", round, hand[i], getImage(hand[i], "original"));

            String subject = String.format("%s: Round %d starting tile",
                                game.getPrefix(), round);

            try {
                web.geekmail(players[i], subject, message);
            } catch (IOException e) {
                LOGGER.throwing("mailTiles()", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void newRound(boolean fresh) {
        round = round + 1;

        deck = new LinkedList<String>((List<String>)game.getData().getProperty(String.format("round%ddeck", round)));

        for (int i = 0; i < NoP; i++) {
            castles[i][0] = 6 - NoP;
            hand[i] = deck.remove(0);
        }

        if (fresh)
            mailTiles();

        for (int i = 0; i < board.length; i++)
            for (int j = 0; j < board[i].length; j++)
                board[i][j] = "empty";

        step = "place";

        // determine starting player
        int max = scores[0];
        turn = 0;
        for (int i = 1; i < NoP; i++) {
            if (scores[i] > max) {
                turn = i;
                max = scores[i];
            }
        }
    }

    private void endRound(boolean fresh) {
        scoreRound(fresh);

        if (round == 3) {
            step = "finished";
            endGame();
        } else {
            newRound(fresh);
        }
    }

    private int scoreCastle(int row, int col, int mult, StringBuilder message) {
        if (message != null)
            message.append("\n- Castle at " + (((char)row + 'A') + "") + (col + 1) + ": ");

        int start, end;
        int mine;
        int value;
        int total = 0;
        boolean dragon, first;
        String tile;

        // score row
        mine = mult;
        dragon = false;
        first = true;
        start = col;
        end = col;

        // find start of scoring area
        while ((tile = getTile(row, start - 1)) != null && !tile.equals("mountain")) {
            start--;
            if (tile.equals("dragon"))
                dragon = true;
            if (tile.equals("goldmine"))
                mine <<= 1;
        }

        // find end of scoring area
        while ((tile = getTile(row, end + 1)) != null && !tile.equals("mountain")) {
            end++;
            if (tile.equals("dragon"))
                dragon = true;
            if (tile.equals("goldmine"))
                mine <<= 1;
        }

        for (int i = start; i <= end; i++) {
            tile = getTile(row, i);
            if (tile.charAt(0) == '+' && !dragon) {
                value = Integer.parseInt(tile);
                if (message != null) {
                    if (first) {
                        message.append("(");
                        first = false;
                    } else {
                        message.append(" + ");
                    }
                    message.append(value);
                }
                total += value * mine;
            } else if (tile.charAt(0) == '-') {
                value = Integer.parseInt(tile);
                if (message != null) {
                    if (first) {
                        message.append("(-");
                        first = false;
                    } else {
                        message.append(" - ");
                    }
                    message.append(-value);
                }
                total += value * mine;
            }
        }

        if (message != null) {
            if (first) {
                message.append("0 * ");
            } else {
                message.append(") * ");
            }
            message.append(mine + " + ");
        }

        // score column
        mine = mult;
        dragon = false;
        first = true;
        start = row;
        end = row;

        // find start of scoring area
        while ((tile = getTile(start - 1, col)) != null && !tile.equals("mountain")) {
            start--;
            if (tile.equals("dragon"))
                dragon = true;
            if (tile.equals("goldmine"))
                mine <<= 1;
        }

        // find end of scoring area
        while ((tile = getTile(end + 1, col)) != null && !tile.equals("mountain")) {
            end++;
            if (tile.equals("dragon"))
                dragon = true;
            if (tile.equals("goldmine"))
                mine <<= 1;
        }

        for (int i = start; i <= end; i++) {
            tile = getTile(i, col);
            if (tile.charAt(0) == '+' && !dragon) {
                value = Integer.parseInt(tile);
                if (message != null) {
                    if (first) {
                        message.append("(");
                        first = false;
                    } else {
                        message.append(" + ");
                    }
                    message.append(value);
                }
                total += value * mine;
            } else if (tile.charAt(0) == '-') {
                value = Integer.parseInt(tile);
                if (message != null) {
                    if (first) {
                        message.append("(-");
                        first = false;
                    } else {
                        message.append(" - ");
                    }
                    message.append(-value);
                }
                total += value * mine;
            }
        }

        if (message != null) {
            if (first) {
                message.append("0 * ");
            } else {
                message.append(") * ");
            }
            message.append(mine + " = " + total);
        }

        return total;
    }

    private void scoreRound(boolean fresh) {
        StringBuilder message = null;

        if (fresh) {
            message = new StringBuilder(getCurrentBoard());
        }

        String tile;
        int mult;

        for (int i = 0; i < NoP; i++) {
            if (fresh) {
                message.append("\n\n[color=#008800]Scoring " + players[i] + ":");
            }

            String color = colors[i];

            for (int row = 0; row < board.length; row++) {
                for (int col = 0; col < board[row].length; col++) {
                    tile = board[row][col];
                    if (tile.startsWith(color)) {
                        mult = Integer.parseInt(tile.substring(color.length()));
                        scores[i] += scoreCastle(row, col, mult, message);
                    }
                }
            }

            if (fresh) {
                message.append("[/color]");
            }
        }

        if (fresh) {
            try {
                web.replyThread(game, new String(message));
            } catch (IOException e) {
                LOGGER.throwing("update()", e);
            }
        }
    }

    private boolean hasCastles(int player) {
        for (int num : castles[player]) {
            if (num > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean boardFull() {
        for (String[] row : board) {
            for (String tile : row) {
                if (tile.equals("empty")) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected CharSequence update() {
        if (!game.inProgress()) {
            return null;
        }
        if (step.equals("place")) {
            StringBuilder message = new StringBuilder(getCurrentStatus());
            message.append('\n').append('\n');
            message.append("[color=purple][b]").append(players[turn]).append(" is up.[/b][/color]\n[color=#008800]Current options:");
            if (deck.size() > 0) {
                message.append("\n[b]draw[/b]");
            }
            if (hand[turn] != null) {
                message.append("\n[b]hand [i]location[/i][/b]");
            }
            if (hasCastles(turn)) {
                message.append("\n[b]castle [i]size[/i] [i]location[/i][/b]");
            }
            message.append("[/color]");
            return message;
        } else if (step.equals("drawn")) {
            return String.format("[color=#008800]%s draws %s[/color]\n%s\n\n[color=#008800]Please [b]place [i]location[/i][/b][/color]", players[turn], deck.get(0), getImage(deck.get(0), "original"));
        } else if (step.equals("colors")) {
            StringBuilder message = new StringBuilder("[color=purple][b]");
            message.append(players[turn]);
            message.append(", please choose a color.[/b][/color]\n[color=#008800]Current options:");

            boolean taken;
            for (String color : ALL_COLORS) {
                if (revColors.get(color) == null) {
                    message.append("\n[b]choose ");
                    message.append(color).append("[/b]");
                }
            }
            message.append("[/color]");
            return message;
        } else {
            LOGGER.warning("Unrecognized step '%s'", step);
            return null;
        }
    }

    @Override
    public void initialize(boolean fresh) {
        board = new String[5][6];
        castles = new int[NoP][4];
        scores = new int[NoP];
        hand = new String[NoP];
        colors = new String[NoP];
        revColors = new HashMap<String, Integer>();

        for (String[] row : board) {
            Arrays.fill(row, "empty");
        }

        for (int i = 0; i < NoP; i++) {
            scores[i] = 50;
            castles[i][1] = 6 - NoP;
            castles[i][1] = 3;
            castles[i][2] = 2;
            castles[i][3] = 1;
        }

        step = "colors";
        turn = 0;
    }

    private void placeTile(String tile, String location) {
        int r = location.charAt(0) - 'A';
        int c = location.charAt(1) - '1';

        board[r][c] = tile;
    }

    private String getTile(int row, int col) {
        if (row < 0 || row >= board.length ||
            col < 0 || col >= board[row].length) {
            return null;
        }

        return board[row][col];
    }

    private String getTile(String location) {
        if (location.length() != 2) {
            return null;
        }

        int r = location.charAt(0) - 'A';
        int c = location.charAt(1) - '1';

        return getTile(r, c);
    }

    private void advanceTurn(boolean fresh) {
        turn++;
        if (step.equals("colors")) {
            if (turn == NoP) {
                newRound(fresh);
            }
        } else if (step.equals("place") || step.equals("drawn")) {
            if (turn == NoP) {
                turn = 0;
            }

            if (boardFull()) {
                endRound(fresh);
            } else if (deck.size() == 0 && hand[turn] == null && !hasCastles(turn)) {
                if (fresh) {
                    try
                    {
                        web.replyThread(game, "[color=#008800]" + players[turn] + " cannot make a legal move and must pass.[/color]");
                    } catch (IOException e) {
                        LOGGER.throwing("advanceTurn()", e);
                    }
                }

                advanceTurn(fresh);
            }
        }
    }

    @Override
    protected void processMove(boolean fresh, String... move) {
        String first = move[0];
        if (step.equals("colors") && first.equals("choose")) {
            colors[turn] = move[1];
            revColors.put(move[1], turn);
            advanceTurn(fresh);
        } else if (step.equals("place")) {
            if (first.equals("draw")) {
                step = "drawn";
            } else if (first.equals("castle")) {
                int size = Integer.parseInt(move[1]);
                placeTile(colors[turn] + move[1], move[2]);
                castles[turn][size - 1]--;
                advanceTurn(fresh);
            } else if (first.equals("hand")) {
                placeTile(hand[turn], move[1]);
                hand[turn] = null;
                advanceTurn(fresh);
            } else {
                LOGGER.warning("Invalid move string '%s'", Utils.join(move, " "));
            }
        } else if (step.equals("drawn") && first.equals("place")) {
            String tile = deck.remove(0);
            placeTile(tile, move[1]);
            step = "place";
            advanceTurn(fresh);
        } else {
            LOGGER.warning("Invalid move string '%s'", Utils.join(move, " "));
        }
    }

    @Override
    public void processCommand(String username, String command) {
        if (step.equals("colors")) {
            if (!username.equals(players[turn])) {
                return;
            }

            if (command.toLowerCase().startsWith("choose ")) {
                String color = command.toLowerCase().substring(7);
                if (revColors.get(color) != null) {
                    LOGGER.info("%s tried to claim %s, which was taken by %s", players[turn], color, players[revColors.get(color)]);
                    return;
                } else if (Arrays.binarySearch(ALL_COLORS, color) > 0) {
                    processAndAddMove("choose", color);
                } else {
                    LOGGER.info("%s tried to claim '%s', an invalid color", players[turn], color);
                }
            }
        } else if (step.equals("place")) {
            if (!username.equals(players[turn])) {
                return;
            }

            if (command.toLowerCase().equals("draw"))
            {
                processAndAddMove("draw");
            } else if (command.toLowerCase().startsWith("hand ")) {
                String location = command.substring(5);
                String tile = getTile(location.toUpperCase());
                if (tile == null) {
                    LOGGER.info("%s tried to place a tile at invalid location '%s'", players[turn], location);
                } else if (!tile.equals("empty")) {
                    LOGGER.info("%s tried to place a tile at occupied location '%s'", players[turn], location);
                } else {
                    processAndAddMove("hand", location.toUpperCase());
                }
            } else if (command.toLowerCase().startsWith("castle ")) {
                String[] pieces = command.substring(7).split(" ");
                if (pieces.length != 2)
                    return;
                String sizeStr = pieces[0];
                String location = pieces[1];
                int size;

                try
                {
                    size = Integer.parseInt(sizeStr);
                    if (size < 1 || size > 4) {
                        LOGGER.info("%s tried to place a castle of invalid size '%s'", players[turn], sizeStr);
                        return;
                    }
                } catch (NumberFormatException e) {
                    LOGGER.info("%s tried to place a castle of invalid size '%s'", players[turn], sizeStr);
                    return;
                }

                if (castles[turn][size - 1] == 0) {
                    LOGGER.info("%s tried to place a castle of size '%s' but has none remaining", players[turn], sizeStr);
                    return;
                }

                String tile = getTile(location.toUpperCase());
                if (tile == null) {
                    LOGGER.info("%s tried to place a tile at invalid location '%s'", players[turn], location);
                } else if (!tile.equals("empty")) {
                    LOGGER.info("%s tried to place a tile at occupied location '%s'", players[turn], location);
                } else {
                    processAndAddMove("castle", sizeStr, location.toUpperCase());
                }
            }
        } else if (step.equals("drawn")) {
            if (!username.equals(players[turn])) {
                return;
            }

            if (command.toLowerCase().startsWith("place ")) {
                String location = command.substring(6);
                String tile = getTile(location.toUpperCase());
                if (tile == null) {
                    LOGGER.info("%s tried to place a tile at invalid location '%s'", players[turn], location);
                } else if (!tile.equals("empty")) {
                    LOGGER.info("%s tried to place a tile at occupied location '%s'", players[turn], location);
                } else {
                    processAndAddMove("place", location.toUpperCase());
                }
            }
        }
    }

    @Override
    public CharSequence getCurrentStatus() {
        if (game.inProgress()) {
            if (step.equals("colors")) {
                return null;
            }

            StringBuilder message = new StringBuilder(getCurrentBoard());
            message.append('\n').append(getCurrentCastles());

            return message;
        }
        return null;
    }

    private CharSequence getCurrentBoard() {
        StringBuilder message = new StringBuilder();

        // print board
        message.append(getImage("header", "original", "inline"));
        for (int i = 0; i < board.length; i++) {
            message.append('\n');
            message.append(getImage("row" + (char)('A' + (char)i), "original", "inline"));
            for (String item : board[i]) {
                message.append(getImage(item, "original", "inline"));
            }
        }

        return message;
    }

    private CharSequence getCurrentCastles() {
        StringBuilder message = new StringBuilder();

        // print scores and castles
        for (int i = 0; i < NoP; i++) {
            // start new row for third and fourth players
            if (i == 2) {
                message.append("[clear]");
            }

            message.append(String.format("[floatleft][center][size=16]%s\n[c]%d[/c][/size]", players[i], scores[i]));
            for (int j = 0; j < castles[i].length; j++) {
                if (castles[i][j] > 0) {
                    message.append('\n');
                }
                for (int k = 0; k < castles[i][j]; k++) {
                    message.append(getImage(colors[i] + (j + 1), "original", "inline"));
                }
            }
            message.append("[/center][/floatleft]");
        }

        message.append("[clear]");

        return message;
    }

    @Override
    public String getHistoryItem(String move) {
        return move;
    }
}
