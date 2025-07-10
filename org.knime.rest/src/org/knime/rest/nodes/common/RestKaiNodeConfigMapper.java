package org.knime.rest.nodes.common;

import java.util.Map;

/**
 * Utility class for mapping configuration POJOs to RestSettings/RestWithBodySettings.
 */
public final class RestKaiNodeConfigMapper {
    private RestKaiNodeConfigMapper() {}

    public static void applyCommonConfig(final RestKaiNodeConfig config, final RestSettings cfg) {
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

        if (cfg instanceof RestWithBodySettings withBodyCfg && config.body != null) {
            withBodyCfg.setUseConstantRequestBody(true);
            withBodyCfg.setConstantRequestBody(config.body);
        }
    }
}
