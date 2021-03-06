package modkiwi.games;

import modkiwi.data.GameInfo;
import modkiwi.util.Logger;
import modkiwi.util.Utils;
import modkiwi.util.WebUtils;
import static modkiwi.util.Constants.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GameBot
{
    private static final Logger LOGGER = new Logger(GameBot.class);

    private static final Pattern P_OTHER = Utils.pat("^\\{([^}]+)\\}\\s*(\\S.*)$");
    private static final Pattern P_SIGNUP = Utils.pat("^sign\\s*up$");
    private static final Pattern P_REMOVE = Utils.pat("^remove$");
    private static final Pattern P_GUESS = Utils.pat("^guess\\s+(\\S.*)$");
    private static final Pattern P_START = Utils.pat("^start$");
    private static final Pattern P_AUTOSTART = Utils.pat("^autostart\\s+(on|off)$");
    private static final Pattern P_MOD = Utils.pat("^(become|relinquish)\\s+mod$");
    private static final Pattern P_COUNT = Utils.pat("^player\\s+count\\s+(\\d+)$");
    private static final Pattern P_STATUS = Utils.pat("^show\\s+status$");
    private static final Pattern P_REPLACE = Utils.pat("^replace\\s+(.*\\S)\\s+with\\s+(.+)$");
    private static final Pattern P_ADD_SETTING = Utils.pat("^add\\s+setting\\s+(\\S.*)$");
    private static final Pattern P_REM_SETTING = Utils.pat("^remove\\s+setting\\s+(\\S.*)$");
    private static final Pattern P_NICKNAME_OTHER = Utils.pat("^nickname\\s+(.*\\S)\\s+as\\s+(.+)$");
    private static final Pattern P_NICKNAME_SELF = Utils.pat("^nickname\\s+(.*)$");

    protected GameInfo game;
    protected final WebUtils web;
    protected int NoP;
    protected String[] players;
    private boolean changed;
    protected List<String> messages = null;
    protected LinkedList<String> historyMessages = null;
    protected List<String> secretMessages = null;

    protected GameBot(GameInfo game) throws IOException {
        this.game = game;
        web = new WebUtils();
        web.login();

        getPlayerData();
    }

    protected void getPlayerData() {
        NoP = game.getPlayers().size();
        players = new String[NoP];
        int k = 0;
        for (String name : game.getPlayers())
            players[k++] = name;
    }

    public abstract void createGame();

    public abstract void initialize(boolean fresh);

    protected abstract CharSequence update();

    private void postUpdate() {
        StringBuilder post = new StringBuilder();
        CharSequence update = update();
        if (messages != null && !messages.isEmpty())
        {
            post.append(Utils.join(messages, "\n\n"));
            if (update != null)
                post.append("\n\n");
        }

        if (update != null)
            post.append(update);

        if (post.length() > 0)
        {
            try
            {
                web.replyThread(game, post);
            }
            catch (IOException e)
            {
                LOGGER.throwing("postUpdate()", e);
            }
        }
        messages.clear();
    }

    private void updateHistory() {
        StringBuilder post = new StringBuilder();

        if (historyMessages != null && !historyMessages.isEmpty()) {
            post.append(Utils.join(historyMessages, "\n\n"));
        }

        if (post.length() > 0) {
            try {
                web.edit(game.getHistoryPost(), "Game History", post);
            } catch (IOException e) {
                LOGGER.throwing("postUpdate()", e);
            }
        }
        messages.clear();
    }

    protected void replace(int index, String newPlayer, boolean fresh) {
        addMessage("[color=purple][b]%s has replaced %s.[/b][/color]", newPlayer, players[index]);
        players[index] = newPlayer;
    }

    protected void globalProcessMove(boolean fresh, String... move) {
        if (move[0].equals("replace"))
        {
            String newPlayer = Utils.join(Arrays.copyOfRange(move, 2, move.length), " ");
            replace(Integer.parseInt(move[1]), newPlayer, fresh);
        }
        else
        {
            processMove(fresh, move);
        }
    }

    protected abstract void processMove(boolean fresh, String... move);

    protected void processAndAddMove(String... move) {
        processAndAddMove(true, move);
    }

    protected void processAndAddMove(boolean change, String... move) {
        globalProcessMove(true, move);
        game.getMoves().add(Utils.join(move, " "));
        if (change)
            changed = true;
    }

    public void startScanning() {
        if (historyMessages == null)
            historyMessages = new LinkedList<String>();

        if (game.inProgress())
            loadGame();

        changed = false;
        messages = new LinkedList<String>();
        secretMessages = new LinkedList<String>();
    }

    public void finishedScanning() {
        if (changed) {
            postUpdate();
            updatePlayerList();
            updateStatus();
            updateHistory();
            changed = false;
            messages = null;
            secretMessages = null;
        }
    }

    public abstract void processCommand(String username, String command);

    public void parseCommand(String username, String command) {
        Matcher m;
        LOGGER.fine("Parsing command '%s' by %s", command, username);
        boolean mod = game.isModerator(username);
        if (mod && (m = P_OTHER.matcher(command)).matches()) {
            parseCommand(m.group(1), m.group(2));
            return;
        } else if (mod && (m = P_NICKNAME_OTHER.matcher(command)).matches()) {
            game.addNickname(m.group(2), m.group(1));
            updatePlayerList();
        } else if ((m = P_NICKNAME_SELF.matcher(command)).matches()) {
            game.addNickname(m.group(1), username);
            updatePlayerList();
        } else if (game.getAcronym() != null &&
                (m = P_GUESS.matcher(command)).matches()) {
            String guess = m.group(1);
            String[] parts = guess.split(" ");
            String[] aparts = game.getAcronym().split(" ");
            int count = 0;
            int len = Math.min(parts.length, aparts.length);
            for (int i = 0; i < len; i++)
                if (parts[i].equalsIgnoreCase(aparts[i]))
                    count++;

            addMessage("[q=\"%s\"][b]%s[/b][/q][color=#008800]%d / %d[/color]", username, guess, count, aparts.length);
            changed = true;
        } else if ((m = P_MOD.matcher(command)).matches()) {
            LOGGER.info("matched mod command");
            if (m.group(1).equalsIgnoreCase("relinquish")) {
                game.getMods().remove(username);
                LOGGER.info("removing mod %s", username);
            } else if (!game.isModerator(username)) {
                game.getMods().add(username);
                LOGGER.info("adding mod %s", username);
            }
        } else if (mod && (m = P_REPLACE.matcher(command)).matches()) {
            int index = getPlayerIndex(m.group(1));
            if (index >= 0)
                processAndAddMove("replace", Integer.toString(index), m.group(2));
        } else if (game.inSignups()) {
            if (P_SIGNUP.matcher(command).matches()) {
                if (!game.getPlayers().contains(username)) {
                    game.getPlayers().add(username);
                    changed = true;
                    if (game.readyToStart()) {
                        startGame();
                    }
                }
            } else if (P_REMOVE.matcher(command).matches()) {
                if (game.getPlayers().remove(username))
                    changed = true;
            } else if (mod && (m = P_AUTOSTART.matcher(command)).matches()) {
                if (m.group(1).equalsIgnoreCase("on")) {
                    game.setAutoStart(true);
                } else {
                    game.setAutoStart(false);
                }
            } else if (mod && (m = P_ADD_SETTING.matcher(command)).matches()) {
                game.addSetting(m.group(1));
            } else if (mod && (m = P_REM_SETTING.matcher(command)).matches()) {
                game.removeSetting(m.group(1));
            } else if (mod && (m = P_COUNT.matcher(command)).matches()) {
                game.setMaxPlayers(Integer.parseInt(m.group(1)));
            } else if (mod && P_START.matcher(command).matches()) {
                startGame();
            } else {
                processCommand(username, command);
            }
        } else if (game.inProgress()) {
            if (P_STATUS.matcher(command).matches()) {
                changed = true;
            } else {
                processCommand(username, command);
            }
        } else {
            processCommand(username, command);
        }
    }

    public void processGeekmail(String username, String subject, String message) {
    }

    public void parseGeekmail(String username, String subject, String message) {
        processGeekmail(username, subject, message);
    }

    public abstract CharSequence getCurrentStatus();

    public void updateStatus() {
        CharSequence status = getCurrentStatus();
        if (game.getStatusPost() != null && status != null) {
            try {
                web.edit(game.getStatusPost(), "Current Status", status);
            } catch (IOException e) {
                LOGGER.throwing("updateStatus()", e);
            }
        }
    }

    public void updatePlayerList() {
        if (game.getSignupPost() == null) {
            return;
        }

        boolean signups = "signups".equals(game.getGameStatus());
        String listText;
        if (signups) {
            Collections.sort(game.getPlayers(), String.CASE_INSENSITIVE_ORDER);
            listText = "[color=#008800][u]Player list according to ModKiwi:[/u]\n";
            for (String username : game.getPlayers()) {
                listText += "[url=" + DOMAIN + "/" + WebUtils.playerThreadURL(game.getThread(), username) + "]" + username + "[/url]\n";
            }

            listText += "\n" + game.getPlayers().size() + " players are signed up.\n\n";
            listText += "To sign up for this game, post [b]signup[/b] in bold.\nTo remove yourself from this game, post [b]remove[/b] in bold.[/color]";
        } else {
            String[] playerList = game.getCurrentPlayers();
            listText = "[color=#008800][u]Seating Order (nicknames):[/u]";
            int k = 1;
            for (String username : playerList) {
                listText += "\n" + k++ + ". [url=" + DOMAIN + "/" + WebUtils.playerThreadURL(game.getThread(), username) + "]" + username + "[/url]";
                List<String> nicknames = game.listNicknames(username);
                if (!nicknames.isEmpty()) {
                    listText += " (" + Utils.join(game.listNicknames(username), ", ") + ")";
                }
            }
            listText += "[/color]";
        }

        try {
            web.edit(game.getSignupPost(), "Player List", listText);
        } catch (IOException e) {
            LOGGER.throwing("updatePlayerList()", e);
        }
    }

    public void startGame() {
        shufflePlayers();
        game.setGameStatus(STATUS_IN_PROGRESS);
        getPlayerData();
        updatePlayerList();
        createGame();
        initialize(true);
        changed = true;
    }

    protected void shufflePlayers() {
        Collections.shuffle(game.getPlayers());
    }

    public void loadGame() {
        if (historyMessages == null)
            historyMessages = new LinkedList<String>();

        initialize(false);
        for (String move : game.getMoves()) {
            globalProcessMove(false, move.split(" "));
        }
    }

    public abstract String getHistoryItem(String move);

    public void endGame() {
        // should probably print something more informative
        try {
            web.replyThread(game.getThread(), null, "[color=purple][b]Game is over.[/b][/color]");
        } catch (IOException e) {
            LOGGER.throwing("endGame()", e);
        }

        game.setGameStatus(STATUS_FINISHED);
    }

    public int getPlayerIndex(String username) {
        for (int i = 0; i < NoP; i++) {
            if (players[i].equalsIgnoreCase(username)) {
                return i;
            }
        }

        return -1;
    }

    protected void addMessage(String format, Object... args) {
        if (messages != null)
            messages.add(String.format(format, args));
    }

    protected void addMessageAndHistory(String format, Object... args) {
        String message = String.format(format, args);
        if (messages != null)
            messages.add(message);
        if (historyMessages != null)
            historyMessages.add(message);
    }

    protected String popHistory() {
        return historyMessages.removeLast();
    }

    protected void addHistory(String format, Object... args) {
        if (historyMessages != null)
            historyMessages.add(String.format(format, args));
    }

    protected void addSecretMessage(String format, Object... args) {
        if (secretMessages != null)
            secretMessages.add(String.format(format, args));
    }

    protected List<String> getSecretReceivers() {
        return game.getNonPlayerMods();
    }

    public void forceUpdate() {
        changed = true;
    }
}
