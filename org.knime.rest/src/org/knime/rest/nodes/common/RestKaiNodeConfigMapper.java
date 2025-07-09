package org.knime.rest.nodes.common;

import java.util.Map;

/**
 * Utility class for mapping configuration POJOs to RestSettings/RestWithBodySettings.
 */
public final class RestKaiNodeConfigMapper {
    private RestKaiNodeConfigMapper() {}

    public static void applyCommonConfig(RestKaiNodeConfig config, RestSettings cfg) {
        cfg.setUseConstantURL(true);
        cfg.setConstantURL(config.url);
        if (config.headers != null) {
            for (Map.Entry<String, String> e : config.headers.entrySet()) {
                cfg.getRequestHeaders().add(new RestSettings.RequestHeaderKeyItem(e.getKey(), e.getValue(), RestSettings.ReferenceType.Constant));
            }
        }
        if (config.followRedirects != null) {
            cfg.setFollowRedirects(config.followRedirects);
        }
        if (config.concurrency != null) {
            cfg.setConcurrency(config.concurrency);
        }
        if (config.connectTimeout != null) {
            cfg.setConnectTimeoutInSeconds(config.connectTimeout);
        }
        if (config.readTimeout != null) {
            cfg.setReadTimeoutInSeconds(config.readTimeout);
        }
        if (config.extractAllResponseHeaders != null) {
            cfg.setExtractAllResponseFields(config.extractAllResponseHeaders);
        }
        if (config.bodyColumnName != null) {
            cfg.setResponseBodyColumn(config.bodyColumnName);
        }
    }

    public static void applyBodyConfig(RestWithBodyKaiNodeConfig config, RestWithBodySettings cfg) {
        if (config.useConstantRequestBody != null) {
            cfg.setUseConstantRequestBody(config.useConstantRequestBody);
        }
        if (config.constantRequestBody != null) {
            cfg.setConstantRequestBody(config.constantRequestBody);
        }
        if (config.requestBodyColumn != null) {
            cfg.setRequestBodyColumn(config.requestBodyColumn);
        }
    }
}
