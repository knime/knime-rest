package org.knime.rest.nodes.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.internal.VariableSettings;
import org.knime.rest.generic.TokenAuthentication;
import org.knime.rest.generic.UsernamePasswordAuthentication;

/**
 * Utility class for mapping configuration POJOs to RestSettings/RestWithBodySettings.
 */
public final class RestKaiNodeConfigMapper {
    private RestKaiNodeConfigMapper() {
    }

    public static List<Consumer<VariableSettings>> applyCommonConfig(final RestKaiNodeConfig config,
        final RestSettings cfg) {
        final List<Consumer<VariableSettings>> variableSetters = new ArrayList<>();
        cfg.setUseConstantURL(true);
        cfg.setConstantURL(config.url);
        if (config.headers != null) {
            for (Map.Entry<String, String> e : config.headers.entrySet()) {
                cfg.getRequestHeaders().add(new RestSettings.RequestHeaderKeyItem(e.getKey(), e.getValue(),
                    RestSettings.ReferenceType.Constant));
            }
        }

        // Handle followRedirects - constant or variable
        if (config.followRedirects != null) {
            if (config.followRedirects.isConstant()) {
                cfg.setFollowRedirects(config.followRedirects.getConstant());
            } else if (config.followRedirects.isVariable()) {
                final String variableName = config.followRedirects.getVariableName();
                variableSetters.add(vs -> {
                    try {
                        vs.addUsedVariable("follow redirects", variableName);
                    } catch (InvalidSettingsException e) {
                        // TODO Auto-generated catch block
                    } // TODO Check with xml
                });
            }
        }

        // Handle concurrency - constant or variable
        if (config.concurrency != null) {
            if (config.concurrency.isConstant()) {
                cfg.setConcurrency(config.concurrency.getConstant());
            } else if (config.concurrency.isVariable()) {
                final String variableName = config.concurrency.getVariableName();
                variableSetters.add(vs -> {
                    try {
                        vs.addUsedVariable("concurrency", variableName);
                    } catch (InvalidSettingsException e) {
                        // TODO Auto-generated catch block
                    }
                });
            }
        }

        // Handle connectTimeout - constant or variable
        if (config.connectTimeout != null) {
            if (config.connectTimeout.isConstant()) {
                cfg.setConnectTimeoutInSeconds(config.connectTimeout.getConstant());
            } else if (config.connectTimeout.isVariable()) {
                final String variableName = config.connectTimeout.getVariableName();
                variableSetters.add(vs -> {
                    try {
                        vs.addUsedVariable("connectTimeout", variableName);
                    } catch (InvalidSettingsException e) {
                        // TODO Auto-generated catch block
                    }
                });
            }
        }

        // Handle readTimeout - constant or variable
        if (config.readTimeout != null) {
            if (config.readTimeout.isConstant()) {
                cfg.setReadTimeoutInSeconds(config.readTimeout.getConstant());
            } else if (config.readTimeout.isVariable()) {
                final String variableName = config.readTimeout.getVariableName();
                variableSetters.add(vs -> {
                    try {
                        vs.addUsedVariable("readTimeout", variableName);
                    } catch (InvalidSettingsException e) {
                        // TODO Auto-generated catch block
                    }
                });
            }
        }

        // Handle extractAllResponseHeaders - constant or variable
        if (config.extractAllResponseHeaders != null) {
            if (config.extractAllResponseHeaders.isConstant()) {
                cfg.setExtractAllResponseFields(config.extractAllResponseHeaders.getConstant());
            } else if (config.extractAllResponseHeaders.isVariable()) {
                final String variableName = config.extractAllResponseHeaders.getVariableName();
                variableSetters.add(vs -> {
                    try {
                        vs.addUsedVariable("Extract all response fields", variableName);
                    } catch (InvalidSettingsException e) {
                        // TODO Auto-generated catch block
                    }
                });
            }
        }

        // Handle bodyColumnName - constant or variable
        if (config.bodyColumnName != null) {
            if (config.bodyColumnName.isConstant()) {
                cfg.setResponseBodyColumn(config.bodyColumnName.getConstant());
            } else if (config.bodyColumnName.isVariable()) {
                final String variableName = config.bodyColumnName.getVariableName();
                variableSetters.add(vs -> {
                    try {
                        vs.addUsedVariable("Body column name", variableName);
                    } catch (InvalidSettingsException e) {
                        // TODO Auto-generated catch block
                    }
                });
            }
        }

        // Handle authentication
        if (config.authentication != null) {
            final String authType = config.authentication.type;
            if (authType != null && !authType.equals("None")) {

                // Find the appropriate authentication configuration and enable it
                for (var authConfig : cfg.getAuthorizationConfigurations()) {
                    if (!authConfig.getName().equals(authType)) {
                        authConfig.setEnabled(false);
                        continue;
                    }
                    // Enable this authentication method
                    authConfig.setEnabled(true);

                    // Handle username/password authentication (Basic, Digest, NTLM)
                    if (authType.equals("Basic")) {
                        final var usernamePasswordAuthConfig = (UsernamePasswordAuthentication)(authConfig.getUserConfiguration());
                        usernamePasswordAuthConfig.setUseCredentials(true);
                        usernamePasswordAuthConfig.setCredential(config.authentication.credentialsVariable.name);
                    }

                    // Handle Bearer token authentication
                    if (authType.equals("Bearer")) {
                        final var tokenAuthConfig = (TokenAuthentication)(authConfig.getUserConfiguration());
                        tokenAuthConfig.setUseCredentials(true);
                        tokenAuthConfig.setCredential(config.authentication.credentialsVariable.name);
                    }
                    break;
                }
            }

        }

        if (cfg instanceof RestWithBodySettings withBodyCfg && config.body != null) {
            withBodyCfg.setUseConstantRequestBody(true);
            withBodyCfg.setConstantRequestBody(config.body);
        }
        return variableSetters;
    }
}
