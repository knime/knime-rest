package org.knime.rest.nodes.common;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.rest.nodes.common.RestSettings.RequestHeaderKeyItem;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Kai interface implementation for REST nodes with a request body (POST, PUT, etc).
 * Exposes settings of {@link RestWithBodySettings} via a JSON schema for LLM configuration.
 */
public abstract class RestWithBodyKaiNodeInterface implements KaiNodeInterface {

    private Supplier<RestWithBodySettings> m_settingsCreator;
    private static final SettingsType MAIN_SETTINGS_TYPE = SettingsType.MODEL;

    protected RestWithBodyKaiNodeInterface(final Supplier<RestWithBodySettings> settingsCreator) {
        m_settingsCreator = settingsCreator;
    }

    // JSON schema for the LLM
    private static final String OUTPUT_SCHEMA = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "title": "RestWithBodyNodeConfiguration",
          "type": "object",
          "properties": {
            "url": { "type": "string", "format": "uri", "description": "Absolute request URL" },
            "headers": { "type": "object", "description": "HTTP header map (string values)", "additionalProperties": { "type": "string" } },
            "followRedirects": { "type": "boolean", "default": true },
            "connectTimeout": { "type": "integer", "minimum": 0, "description": "Connection timeout in seconds" },
            "readTimeout": { "type": "integer", "minimum": 0, "description": "Read timeout in seconds" },
            "concurrency": { "type": "integer", "minimum": 1, "maximum": 32, "description": "Number of parallel requests" },
            "extractAllResponseHeaders": { "type": "boolean", "default": false },
            "bodyColumnName": { "type": "string", "description": "Name of the output column holding the response body", "default": "body" },
            "useConstantRequestBody": { "type": "boolean", "default": true, "description": "Whether to use a constant request body" },
            "constantRequestBody": { "type": "string", "description": "The constant request body to send (if enabled)" },
            "requestBodyColumn": { "type": "string", "description": "Name of the input column containing the request body (if not using constant)" }
          },
          "required": ["url"],
          "additionalProperties": false
        }
        """;

    @Override
    public Set<SettingsType> getSettingsTypes() {
        return Collections.singleton(MAIN_SETTINGS_TYPE);
    }

    @Override
    public ConfigurePrompt getConfigurePrompt(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
            final PortObjectSpec[] specs) {
        final String systemMessage = """
                You are configuring a KNIME REST node (POST/PUT/...). Return **only** a JSON object matching the schema provided. Populate at least the mandatory \"url\" field and, where useful, headers, time‑outs, and request body fields. Do not wrap the JSON in markdown, prose, or back‑ticks – the raw JSON is required.
                """;
        return new ConfigurePrompt(systemMessage, OUTPUT_SCHEMA);
    }

    @Override
    public void applyConfigureResponse(final String response,
            final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
            final Map<SettingsType, NodeAndVariableSettingsWO> settings) {
        if (response == null || response.isBlank()) {
            return;
        }
        final RestWithBodyKaiNodeConfig config;
        try {
            JsonObject root = JsonParser.parseString(response.trim()).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                throw new IllegalArgumentException("Response JSON must have a 'data' object property");
            }
            config = new Gson().fromJson(root.getAsJsonObject("data"), RestWithBodyKaiNodeConfig.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Model response is not valid JSON or does not match expected schema: " + ex.getMessage(), ex);
        }
        if (config == null || config.url == null || config.url.isBlank()) {
            throw new IllegalArgumentException("Expected a JSON object with at least a non-empty 'url' field as response from the model.");
        }
        final RestWithBodySettings cfg = m_settingsCreator.get();
        RestKaiNodeConfigMapper.applyCommonConfig(config, cfg);
        RestKaiNodeConfigMapper.applyBodyConfig(config, cfg);
        final NodeAndVariableSettingsWO dialogWO = settings.get(MAIN_SETTINGS_TYPE);
        if (dialogWO == null) {
            throw new IllegalStateException("Writable settings for DIALOG are missing");
        }
        cfg.saveSettings(dialogWO);
    }
}
