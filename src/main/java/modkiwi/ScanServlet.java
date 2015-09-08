package modkiwi;

import modkiwi.data.ArticleInfo;
import modkiwi.data.GameInfo;
import modkiwi.data.ThreadInfo;
import modkiwi.games.BotManager;
import modkiwi.games.GameBot;
import modkiwi.util.DatastoreUtils;
import modkiwi.util.Logger;
import modkiwi.util.Utils;
import modkiwi.util.WebUtils;
import static modkiwi.util.Constants.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.*;

public class ScanServlet extends HttpServlet
{
    private static final Logger LOGGER = new Logger(ScanServlet.class);
    private static final Object scanLock = new Object();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        synchronized (scanLock)
        {
            resp.setContentType("text/plain");
            PrintWriter pw = resp.getWriter();

            WebUtils web = new WebUtils();
            web.login();

            for (GameInfo game : DatastoreUtils.gamesByStatus(STATUS_IN_SIGNUPS, STATUS_IN_PROGRESS, STATUS_FINISHED))
            {
                LOGGER.fine("Scanning %s", game.getFullTitle());

                ThreadInfo ti;
                if (game.getLastScanned() != null)
                {
                    ti = web.getThread(game.getThread(), Integer.toString(Integer.parseInt(game.getLastScanned()) + 1));
                }
                else
                {
                    ti = web.getThread(game.getThread());
                }

                LOGGER.finer("%d new articles for %s", ti.getArticles().length, game.getFullTitle());

                if (ti.getArticles().length == 0)
                    continue;

                GameBot bot = BotManager.getBot(game);
                bot.startScanning();

                ArticleInfo[] articles = ti.getArticles();
                for (ArticleInfo article : articles)
                {
                    String username = article.getUsername();
                    if (username.equals(web.getUsername()))
                        continue;
                    for (String command : article.getCommands())
                    {
                        bot.parseCommand(username, command);
                    }
                }

                bot.finishedScanning();
                game.setLastScanned(articles[articles.length - 1].getId());
                game.save();
            }
        }
    }
}
