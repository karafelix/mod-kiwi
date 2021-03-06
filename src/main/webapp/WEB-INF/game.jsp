<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="modkiwi.data.GameInfo" %>
<%@ page import="modkiwi.util.WebUtils" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
GameInfo game = (GameInfo)request.getAttribute("gameInfo");
%>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="../bgg.css" />
    <script src="../webjars/jquery/2.1.4/jquery.min.js"> </script>
    <title>Modkiwi - <%= game.getFullTitle() %></title>
</head>

<body>
    <h1><div id='name_span'><%= game.getFullTitle() %></div></h1>
    <div id='links'>
        <a href="https://boardgamegeek.com/thread/<%= game.getThread() %>">Go to game thread</a> (<a href="https://boardgamegeek.com/thread/<%= game.getThread() %>/new">latest)
    </div>
    <div id='player_table'>
        <table border="0" class="forum_table" cellpadding="4" cellspacing="2">
            <tr>
                <th>Players (<%= game.getPlayers().size() %>)</th>
            </tr>
            <% for (String player : game.getCurrentPlayers()) { %>
            <tr>
				<td><a href="../<%= WebUtils.playerThreadURL(game.getThread(), player) %>"><%= player %></a></td>
            </tr>
            <% } %>
        </table>
    </div>
</body>
</html>
