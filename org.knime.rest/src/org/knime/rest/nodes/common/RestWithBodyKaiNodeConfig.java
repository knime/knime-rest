package org.knime.rest.nodes.common;

import java.util.Map;

/**
 * POJO for the configuration response from the LLM for RestWithBodyKaiNodeInterface.
 */
public class RestWithBodyKaiNodeConfig extends RestKaiNodeConfig {
    public Boolean useConstantRequestBody;
    public String constantRequestBody;
    public String requestBodyColumn;
}
