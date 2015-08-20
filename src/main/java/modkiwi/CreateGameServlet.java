package modkiwi;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Transaction;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

import javax.servlet.http.*;

public class CreateGameServlet extends HttpServlet
{
    private static final Random rand = new Random();

    private String generateId()
    {
        return Integer.toHexString(rand.nextInt());
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();

        PrintWriter pw = resp.getWriter();
        resp.setContentType("text/plain");

        try
        {
            Entity ent = new Entity("Game", generateId());
            ent.setProperty("gametype", req.getParameter("gametype"));
            ent.setProperty("index", req.getParameter("index"));
            ent.setProperty("title", req.getParameter("name"));
            ent.setProperty("acronym", req.getParameter("acronym"));
            ent.setProperty("thread", req.getParameter("thread"));
            if (req.getParameter("mods") == null)
                ent.setProperty("mods", new ArrayList<String>());
            else
                ent.setProperty("mods", Arrays.asList(req.getParameter("mods").split(",")));
            ent.setProperty("signup", req.getParameter("signup"));
            ent.setProperty("current_status", req.getParameter("status"));
            ent.setProperty("history", req.getParameter("history"));
            ent.setProperty("players", new ArrayList<String>());
            ent.setProperty("data", null);
            ent.setProperty("game_status", "signups");
            datastore.put(ent);
            txn.commit();
        }
        finally
        {
            if (txn.isActive())
                txn.rollback();
        }
    }
}
