package modkiwi.util;

import modkiwi.data.GameInfo;
import modkiwi.data.GeekMailInfo;
import modkiwi.data.ThreadInfo;
import modkiwi.data.BGGUserInfo;
import modkiwi.net.NetConnection;
import modkiwi.net.RequestBuilder;
import modkiwi.net.WebConnection;
import modkiwi.net.WebRequest;
import modkiwi.net.WebResponse;
import modkiwi.net.exceptions.UnexpectedResponseCodeException;
import modkiwi.util.Utils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.urlfetch.*;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebUtils
{
    private String username;
    private WebConnection conn;
    private static final Pattern REPLY_PATTERN = Pattern.compile("^\\[q=\"[^\"]*\"\\](.*)\\[/q\\]$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Logger LOGGER = new Logger(WebUtils.class);

    public WebUtils() {
        conn = new NetConnection();
    }

    public WebUtils(WebConnection conn) {
        this.conn = conn;
    }

    public synchronized WebResponse login() throws IOException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey("Credentials", "autobot");
        try {
            Entity ent = datastore.get(key);
            this.username = ent.getProperty("username").toString();
            String password = ent.getProperty("password").toString();
            return login(username, password);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public synchronized WebResponse login(String username, String password) throws IOException {
        WebRequest request = RequestBuilder.post()
                .setUrl("http://boardgamegeek.com/login")
                .addParameter("username", username)
                .addParameter("password", password)
                .build();

        WebResponse resp = conn.execute(request);
        conn.saveCookies();
        return resp;
    }

    public String getUsername() {
        return username;
    }

    public synchronized WebResponse geekmail(String user, String subject, CharSequence content) throws IOException {
        WebRequest request = RequestBuilder.post()
                .setUrl("http://boardgamegeek.com/geekmail_controller.php")
                .addParameter("B1", "Send")
                .addParameter("action", "save")
                .addParameter("body", content)
                .addParameter("savecopy", "1")
                .addParameter("subject", subject)
                .addParameter("touser", user)
                .build();

        return conn.execute(request);
    }

    public synchronized WebResponse geekmail(List<String> users, String subject, CharSequence content) throws IOException {
        if (users == null || users.size() == 0)
            return null;

        String userString = Utils.join(users, ",");

        return geekmail(userString, subject, content);
    }

    public synchronized void thumb(String article) throws IOException {
        WebRequest request = RequestBuilder.post()
                .setUrl("https://boardgamegeek.com/geekrecommend.php")
                .addParameter("action", "recommend")
                .addParameter("itemid", article)
                .addParameter("itemtype", "article")
                .addParameter("value", "1")
                .build();

        WebResponse response = conn.execute(request);
    }

    public synchronized void edit(String article, String subject, CharSequence content) throws IOException {
        WebRequest request = RequestBuilder.post()
                .setUrl("https://boardgamegeek.com/article/save")
                .addParameter("action", "save")
                .addParameter("articleid", article)
                .addParameter("subject", subject)
                .addParameter("body", content)
                .addParameter("B1", "Submit")
                .build();

        WebResponse response = conn.execute(request);
    }

    public synchronized String replyArticle(String article, String subject, CharSequence content) throws IOException {
        WebRequest request = RequestBuilder.post()
                .setUrl("https://boardgamegeek.com/article/save")
                .addParameter("action", "save")
                .addParameter("replytoid", article)
                .addParameter("subject", subject)
                .addParameter("body", content)
                .build();

        WebResponse response = conn.execute(request);

        Pattern pattern = Pattern.compile("boardgamegeek.com/article/(\\d*)#(\\d*)$");
        Matcher matcher = pattern.matcher(response.getFinalUrl());

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public synchronized String replyThread(String thread, String subject, CharSequence content) throws IOException {
        ThreadInfo t = getThread(thread, 1);
        String sub = subject;
        if (sub == null) {
            sub = "Re: " + t.getSubject();
        }

        String article = t.getArticleId();

        WebRequest request = RequestBuilder.post()
                .setUrl("https://boardgamegeek.com/article/save")
                .addParameter("action", "save")
                .addParameter("replytoid", article)
                .addParameter("subject", sub)
                .addParameter("body", content)
                .build();

        WebResponse response = conn.execute(request);

        Pattern pattern = Pattern.compile("boardgamegeek.com/article/(\\d*)#(\\d*)$");
        Matcher matcher = pattern.matcher(response.getFinalUrl());

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public synchronized String replyThread(GameInfo game, CharSequence content) throws IOException {
        return replyThread(game.getThread(), null, content);
    }

    public synchronized ThreadInfo getThread(String thread, String article, int max) throws IOException {
        WebRequest request = RequestBuilder.get()
                .setUrl("https://boardgamegeek.com/xmlapi2/thread")
                .addParameter("id", thread)
                .addParameter("minarticle", article)
                .addParameter("count", Integer.toString(max))
                .build();

        return getThread(request);
    }

    public synchronized ThreadInfo getThread(String thread, String article) throws IOException {
        WebRequest request = RequestBuilder.get()
                .setUrl("https://boardgamegeek.com/xmlapi2/thread")
                .addParameter("id", thread)
                .addParameter("minarticleid", article)
                .build();

        return getThread(request);
    }

    public synchronized ThreadInfo getThread(String thread, int max) throws IOException {
        WebRequest request = RequestBuilder.get()
                .setUrl("https://boardgamegeek.com/xmlapi2/thread")
                .addParameter("id", thread)
                .addParameter("count", Integer.toString(max))
                .build();

        return getThread(request);
    }

    public synchronized ThreadInfo getThread(String thread) throws IOException {
        WebRequest request = RequestBuilder.get()
                .setUrl("https://boardgamegeek.com/xmlapi2/thread")
                .addParameter("id", thread)
                .build();

        return getThread(request);
    }

    public synchronized ThreadInfo getThread(WebRequest request) throws IOException {
        try {
            WebResponse response = conn.execute(request);
            Element node = response.parse().select("thread").first();
            if (node == null) {
                LOGGER.warning("Request to %s had no thread node.", request.toString());
                return new ThreadInfo();
            }
            return new ThreadInfo(node);
        } catch (SocketTimeoutException e) {
            LOGGER.warning("Request to " + request.toString() + " timed out.");
            return new ThreadInfo();
        } catch (UnexpectedResponseCodeException e) {
            LOGGER.warning("Request to " + request.toString() + " resulted in unexpected response code " + e.getResponseCode());
            return new ThreadInfo();
        }
    }

    public synchronized BGGUserInfo getUserInfo(String user) throws IOException {
        WebRequest request = RequestBuilder.get()
                .setUrl("https://boardgamegeek.com/xmlapi2/users")
                .addParameter("name", user)
                .build();

        WebResponse response = conn.execute(request);
        return new BGGUserInfo(response.parse().select("user").first());
    }

    public synchronized LinkedList<GeekMailInfo> getMail(String gameId) throws IOException {
        return getMail(gameId, null);
    }

    public synchronized LinkedList<GeekMailInfo> getMail(String gameId, String lastMessage) throws IOException {
        WebRequest request = RequestBuilder.get()
                .setUrl("http://boardgamegeek.com/geekmail_controller.php")
                .addParameter("action", "search")
                .addParameter("folder", "inbox")
                .addParameter("search", "\\[" + gameId + "\\]")
                .build();

        WebResponse response = conn.execute(request);

        Elements tables = response.parse().select("div#mychecks table.gm_messages");

        int minValue;
        if (lastMessage == null) {
            minValue = Integer.MIN_VALUE;
        } else {
            minValue = Integer.parseInt(lastMessage);
        }

        LinkedList<GeekMailInfo> list = new LinkedList<GeekMailInfo>();
        for (Element e : tables) {
            GeekMailInfo item = new GeekMailInfo(this, e);
            if (Integer.parseInt(item.getId()) > minValue) {
                list.addFirst(item);
            } else {
                break;
            }
        }

        return list;
    }

    public synchronized String getMailContent(String messageid) throws IOException {
        WebRequest request = RequestBuilder.get()
                .setUrl("http://boardgamegeek.com/geekmail_controller.php")
                .addParameter("action", "reply")
                .addParameter("messageid", messageid)
                .build();

        String text = conn.execute(request).parse().select("textarea#body").first().text();
        Matcher m = REPLY_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }

    public static String playerThreadURL(String thread, String player) {
        return "thread?thread=" + thread + "&username=" + player.replace(" ", "%20");
    }

    public static String threadURLNew(String thread, String player) {
        return "https://boardgamegeek.com/thread/" + thread + "/new";
    }
}
