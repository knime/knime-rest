package org.knime.rest.nodes.common;

import java.util.Map;

/**
 * POJO for the configuration response from the LLM for RestKaiNodeInterface.
 */
public class RestKaiNodeConfig {
    public String url;
    public String body;
    public Map<String, String> headers;
    public ConstantOrVariable<Boolean> followRedirects;
    public ConstantOrVariable<Integer> connectTimeout;
    public ConstantOrVariable<Integer> readTimeout;
    public ConstantOrVariable<Integer> concurrency;
    public ConstantOrVariable<Boolean> extractAllResponseHeaders;
    public ConstantOrVariable<String> bodyColumnName;
    public Authentication authentication;

    /**
     * Authentication configuration for the REST request.
     */
    public static class Authentication {
        public String type; // "None", "Basic", "Bearer"
        public CredentialsVariable credentialsVariable;

        public static class CredentialsVariable {
            public String name;
            public String title;
            public String description;
            public String usernamePlaceholder;
            public String passwordOrTokenPlaceholder;
        }
    }
}
