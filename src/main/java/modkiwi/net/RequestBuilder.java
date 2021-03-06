package modkiwi.net;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RequestBuilder
{
    private static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();
    private Map<String, String> params;
    private String url;
    private String requestType;
    private String charset;

    private RequestBuilder(String requestType)
    {
        params = new HashMap<String, String>();
        url = null;
        this.requestType = requestType;
        charset = DEFAULT_CHARSET;
    }

    public static RequestBuilder get()
    {
        return new RequestBuilder("GET");
    }

    public static RequestBuilder post()
    {
        return new RequestBuilder("POST");
    }

    public RequestBuilder setUrl(String url)
    {
        this.url = url;
        return this;
    }

    public RequestBuilder addParameter(String key, CharSequence value)
    {
        params.put(key, value.toString());
        return this;
    }

    public WebRequest build() throws IOException
    {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet())
        {
            if (query.length() > 0)
                query.append('&');
            query.append(URLEncoder.encode(entry.getKey(), charset));
            query.append('=');
            query.append(URLEncoder.encode(entry.getValue(), charset));
        }
        return new WebRequest(url, query.toString(), requestType, charset);
    }
}
