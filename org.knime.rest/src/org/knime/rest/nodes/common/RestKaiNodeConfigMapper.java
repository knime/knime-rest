package org.knime.rest.nodes.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.internal.VariableSettings;

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
                    if (authConfig.getName().equals(authType)) {
                        // Disable all other authentication methods
                        for (var otherAuth : cfg.getAuthorizationConfigurations()) {
                            otherAuth.setEnabled(false);
                        }
                        // Enable this authentication method
                        authConfig.setEnabled(true);

                        // Handle username/password authentication (Basic, Digest, NTLM)
                        if (authType.equals("Basic") || authType.equals("Digest") || authType.equals("NTLM (Labs)")) {
                            if (config.authentication.username != null) {
                                if (config.authentication.username.isVariable()) {
                                    final String variableName = config.authentication.username.getVariableName();
                                    final String authKey = authType + " auth";
                                    variableSetters.add(vs -> {
                                        try {
                                            //vs.getOrCreateVariableSettings(authKey).addUsedVariable("username",
                                            //    variableName);
                                        } catch (Exception e) {
                                            // TODO Auto-generated catch block
                                        }
                                    });
                                }
                            }

                            if (config.authentication.password != null) {
                                if (config.authentication.password.isVariable()) {
                                    final String variableName = config.authentication.password.getVariableName();
                                    final String authKey = authType + " auth";
                                    variableSetters.add(vs -> {
                                        try {
                                            //vs.getOrCreateVariableSettings(authKey).addUsedVariable("password",
                                            //    variableName);
                                        } catch (Exception e) {
                                            // TODO Auto-generated catch block
                                        }
                                    });
                                }
                            }

                            // Handle domain for NTLM
                            if (authType.equals("NTLM (Labs)") && config.authentication.domain != null) {
                                if (config.authentication.domain.isVariable()) {
                                    final String variableName = config.authentication.domain.getVariableName();
                                    variableSetters.add(vs -> {
                                        try {
                                           // vs.getOrCreateVariableSettings("NTLM-auth");
                                            //vs
                                            //    .addUsedVariable("org.knime.rest.auth.ntlm.domain", variableName);
                                        } catch (Exception e) {
                                            // TODO Auto-generated catch block
                                        }
                                    });
                                }
                            }
                        }

                        // Handle Bearer token authentication
                        if (authType.equals("Bearer") && config.authentication.token != null) {
                            if (config.authentication.token.isVariable()) {
                                final String variableName = config.authentication.token.getVariableName();
                                variableSetters.add(vs -> {
                                    try {
                                        //vs.getOrCreateVariableSettings("Bearer auth").addUsedVariable("password",
                                        //    variableName);
                                    } catch (Exception e) {
                                        // TODO Auto-generated catch block
                                    }
                                });
                            }
                        }
                        break;
                    }
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
