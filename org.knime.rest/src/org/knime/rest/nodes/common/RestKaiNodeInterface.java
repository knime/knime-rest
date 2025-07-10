/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 9, 2025 (benjaminwilhelm): created
 */
package org.knime.rest.nodes.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsRO;
import org.knime.core.webui.node.dialog.NodeAndVariableSettingsWO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.rest.nodes.KaiNodeInterfaceWithComponentWrapper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Kai interface implementation for the GET Request node. The implementation exposes the most important settings of
 * {@link RestGetSettings} via a JSON schema so that KNIME AI Build‑Mode can configure the node programmatically. Only a
 * relevant subset of the nearly 100 available settings is surfaced — enough for the majority of real‑world GET calls.
 *
 * <p>
 * Supported JSON fields (all optional unless stated otherwise):
 * <ul>
 * <li><b>url</b>  (string, required) – full request URL</li>
 * <li><b>headers</b> (object) – map of HTTP header key/value pairs</li>
 * <li><b>followRedirects</b> (boolean)</li>
 * <li><b>connectTimeout</b> (integer, seconds)</li>
 * <li><b>readTimeout</b> (integer, seconds)</li>
 * <li><b>concurrency</b> (integer) – parallel requests (1 – 32)</li>
 * <li><b>extractAllResponseHeaders</b> (boolean)</li>
 * <li><b>bodyColumnName</b> (string)</li>
 * </ul>
 * Any additional keys are ignored.
 */
public abstract class RestKaiNodeInterface implements KaiNodeInterface, KaiNodeInterfaceWithComponentWrapper {

    // TODO make it work for other HTTP methods than GET

    private final Supplier<RestSettings> m_settingsCreator;

    private final Method m_method;

    private final boolean m_hasBody;

    protected enum Method {
            GET, POST, PUT, DELETE;
    }

    protected RestKaiNodeInterface(final Supplier<RestSettings> settingsCreator, final Method method) {
        m_settingsCreator = settingsCreator;
        m_method = method;
        m_hasBody = m_method != Method.GET;
    }

    private static final SettingsType MAIN_SETTINGS_TYPE = SettingsType.MODEL;

    // --- JSON schema that the model must follow ----------------------------------------------
    private String getOutputSchema() {
        return """
                            {
                    "$schema": "http://json-schema.org/draft-07/schema#",
                    "title": "RestNodeConfiguration",
                    "type": "object",
                    "properties": {
                        "configuration": {
                            "type": "object",
                            "properties": {
                                "url": {
                                    "type": "string",
                                    "format": "uri",
                                    "description": "Absolute request URL. Supports templating via `{{var-name}}` with variables defined in `templateVariables`."
                                },
                                %s
                                "headers": {
                                    "type": "object",
                                    "description": "HTTP header map (string values)",
                                    "additionalProperties": {
                                        "type": "string"
                                    }
                                },
                                "followRedirects": {
                                    "type": "boolean",
                                    "default": true
                                },
                                "connectTimeout": {
                                    "type": "integer",
                                    "minimum": 0,
                                    "description": "Connection timeout in seconds"
                                },
                                "readTimeout": {
                                    "type": "integer",
                                    "minimum": 0,
                                    "description": "Read timeout in seconds"
                                },
                                "concurrency": {
                                    "type": "integer",
                                    "minimum": 1,
                                    "maximum": 32,
                                    "description": "Number of parallel requests"
                                },
                                "extractAllResponseHeaders": {
                                    "type": "boolean",
                                    "default": false
                                },
                                "bodyColumnName": {
                                    "type": "string",
                                    "description": "Name of the output column holding the response body",
                                    "default": "body"
                                }
                            },
                            "required": [
                                "url"
                            ],
                            "additionalProperties": false
                        },
                        "templateVariables": {
                            "type": "array",
                            "items": {
                                "type": "object",
                                "properties": {
                                    "name": {
                                        "type": "string",
                                        "description": "Variable name. Use kebap case. This has to match the names used in template syntax in configuration settings."
                                    },
                                    "title": {
                                        "type": "string",
                                        "description": "Variable title"
                                    },
                                    "description": {
                                        "type": "string",
                                        "description": "Variable description"
                                    },
                                    "defaultValue": {
                                        "type": [
                                            "string",
                                            "number",
                                            "boolean"
                                        ],
                                        "description": "Default value for the variable"
                                    },
                                    "type": {
                                        "type": "string",
                                        "enum": [
                                            "STRING",
                                            "DOUBLE",
                                            "INT",
                                            "BOOLEAN"
                                        ],
                                        "description": "Type of the variable"
                                    }
                                }
                            }
                        }
                    },
                    "required": [
                        "configuration"
                    ],
                    "additionalProperties": false
                }
                       """.formatted(m_hasBody ? """
                                 "body": {
                                    "type": "string",
                                    "description": "Request body. Supports templating via `{{var-name}}` with variables defined in `templateVariables`."
                                },

                               """ : "");
    }

    // -----------------------------------------------------------------------------------------

    @Override
    public Set<SettingsType> getSettingsTypes() {
        return Collections.singleton(MAIN_SETTINGS_TYPE);
    }

    @Override
    public ConfigurePrompt getConfigurePrompt(final Map<SettingsType, NodeAndVariableSettingsRO> settings,
        final PortObjectSpec[] specs) {
        // A concise system message instructing the LLM what to do and how to respond.
        final String systemMessage = """
                You are configuring a KNIME \"%s Request\" node. Return **only** a JSON object matching the
                schema provided. Populate at least the mandatory \"url\" field and, where useful, headers or
                time‑outs. Do not wrap the JSON in markdown, prose, or back‑ticks – the raw JSON is required.
                """.formatted(m_method.name());
        return new ConfigurePrompt(systemMessage, getOutputSchema());
    }

    @Override
    public void applyConfigureResponse(final String response,
        final Map<SettingsType, NodeAndVariableSettingsRO> previousSettings,
        final Map<SettingsType, NodeAndVariableSettingsWO> settings) {

        if (response == null || response.isBlank()) {
            return; // nothing to apply
        }

        // Parse the JSON returned by the model, expecting {"data": {...}}
        final RestKaiNodeConfig config;
        try {
            JsonObject root = JsonParser.parseString(response.trim()).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                throw new IllegalArgumentException("Response JSON must have a 'data' object property");
            }
            config = new Gson().fromJson(root.getAsJsonObject("data").get("configuration"), RestKaiNodeConfig.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                "Model response is not valid JSON or does not match expected schema: " + ex.getMessage(), ex);
        }
        if (config == null || config.url == null || config.url.isBlank()) {
            throw new IllegalArgumentException(
                "Expected a JSON object with at least a non-empty 'url' field as response from the model.");
        }

        // Build RestGetSettings from the POJO
        final RestSettings cfg = m_settingsCreator.get();
        RestKaiNodeConfigMapper.applyCommonConfig(config, cfg);

        // Finally write the settings
        final NodeAndVariableSettingsWO dialogWO = settings.get(MAIN_SETTINGS_TYPE);
        if (dialogWO == null) {
            throw new IllegalStateException("Writable settings for DIALOG are missing");
        }
        cfg.saveSettings(dialogWO);
    }

    /**
     * POJO for the configuration response from the LLM for RestKaiNodeInterface.
     */
    public static class ConfigurationAndTemplate {
        public RestKaiNodeConfig configuration;

        public List<Variable> templateVariables;
    }

    public static class Variable {
        public String name;

        public String title;

        public String description;

        public Object defaultValue;

        public VariableType type;

        enum VariableType {
                STRING, DOUBLE, INT, BOOLEAN;
        }
    }

    @Override
    public Optional<SubNodeContainer> wrapAndConfigureNode(final NodeContainer nc, final String response) {
        // receive the templateVaraibles field in the response and if they do not exist, we return empty. If they do, we parse the configuration as above and resolve it customly

        if (response == null || response.isBlank()) {
            return Optional.empty(); // nothing to apply
        }

        // Parse the JSON returned by the model, expecting {"data": {...}}
        final ConfigurationAndTemplate configAndTemplateVariables;
        try {
            JsonObject root = JsonParser.parseString(response.trim()).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                throw new IllegalArgumentException("Response JSON must have a 'data' object property");
            }
            configAndTemplateVariables =
                new Gson().fromJson(root.getAsJsonObject("data"), ConfigurationAndTemplate.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                "Model response is not valid JSON or does not match expected schema: " + ex.getMessage(), ex);
        }

        final var templateVariables = configAndTemplateVariables.templateVariables;
        final var config = configAndTemplateVariables.configuration;

        if (templateVariables == null || templateVariables.isEmpty()) {
            return Optional.empty();
        }

        // Build RestGetSettings from the POJO
        final RestSettings cfg = m_settingsCreator.get();
        RestKaiNodeConfigMapper.applyCommonConfig(config, cfg);

        final var wfm = nc.getParent();
        final var nodeSettings = new NodeSettings("http node settings");
        wfm.saveNodeSettings(nc.getID(), nodeSettings);

        // Finally write the settings
        final var dialogWO = nodeSettings.addNodeSettings(MAIN_SETTINGS_TYPE.getConfigKey());
        if (dialogWO == null) {
            throw new IllegalStateException("Writable settings for DIALOG are missing");
        }
        cfg.saveSettings(dialogWO);

        try {
            wfm.loadNodeSettings(nc.getID(), nodeSettings);
        } catch (InvalidSettingsException e) {
            // TODO (LOGGER + error response)
        }

        return Optional
            .of(WrapHTTPNodeUtil.configureHTTPNodeAndResolveTemplates((NativeNodeContainer)nc, templateVariables, nodeSettings, cfg));

    }

}
