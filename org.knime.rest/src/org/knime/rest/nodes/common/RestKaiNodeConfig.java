package org.knime.rest.nodes.common;

import java.util.Map;

/**
 * POJO for the configuration response from the LLM for RestKaiNodeInterface.
 */
public class RestKaiNodeConfig {
    public String url;
    public String body;
    public Map<String, String> headers;
    public Boolean followRedirects;
    public Integer connectTimeout;
    public Integer readTimeout;
    public Integer concurrency;
    public Boolean extractAllResponseHeaders;
    public String bodyColumnName;
}
