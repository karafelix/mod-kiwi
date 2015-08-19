package modkiwi.net;

import java.net.*;

public class Request
{
    private String url, query, requestType, charset;

    public Request(String url, String query, String requestType, String charset)
    {
        this.url = url;
        this.query = query;
        this.requestType = requestType;
        this.charset = charset;
    }

    public String getUrl()
    {
        return url;
    }

    public String getQuery()
    {
        return query;
    }

    public String getRequestType()
    {
        return requestType;
    }

    public String getCharset()
    {
        return charset;
    }
}
