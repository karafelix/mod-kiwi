<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="java.util.Map" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<% Map<String, String> gametypes = (Map<String, String>)request.getAttribute("gametypes"); %>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="../main.css" />
    <script src="../webjars/jquery/2.1.4/jquery.min.js"> </script>
</head>

<body>
    <form action="create" method="post">
        Game Type:
        <select name="gametype">
            <% for (Map.Entry<String, String> entry : gametypes.entrySet()) { %>
            <option value="<%= entry.getKey() %>"><%= entry.getValue() %></option>
            <% } %>
        </select>
        <br>
        Index: <input type="text" name="index"><br>
        Name: <input type="text" name="name"><br>
        Acronym: <input type="text" name="acronym"><br>
        Thread: <input type="text" name="thread"><br>
        Player Count: <input type="text" name="max_players"><br>
        Moderator(s): <input type="text" name="mods"><br>
        Signup Post: <input type="text" name="signup"><br>
        Status Post: <input type="text" name="status"><br>
        History Post: <input type="text" name="history"><br>
        <input type="submit" value="Submit">
    </form>
</body>
</html>