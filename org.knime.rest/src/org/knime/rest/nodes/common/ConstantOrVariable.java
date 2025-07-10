package org.knime.rest.nodes.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Wrapper class that can hold either a constant value or a variable name reference.
 * Used for REST node configuration fields that can either be set to a constant value
 * or reference a template variable.
 *
 * @param <T> The type of the constant value
 */
@JsonDeserialize(using = ConstantOrVariable.ConstantOrVariableDeserializer.class)
public class ConstantOrVariable<T> {
    private final T constant;
    private final String variableName;

    private ConstantOrVariable(final T constant, final String variableName) {
        this.constant = constant;
        this.variableName = variableName;
    }

    /**
     * Creates a ConstantOrVariable with a constant value.
     *
     * @param constant The constant value
     * @param <T> The type of the constant value
     * @return A new ConstantOrVariable instance
     */
    public static <T> ConstantOrVariable<T> ofConstant(final T constant) {
        return new ConstantOrVariable<>(constant, null);
    }

    /**
     * Creates a ConstantOrVariable with a variable name reference.
     *
     * @param variableName The name of the template variable
     * @param <T> The type of the constant value (for type safety)
     * @return A new ConstantOrVariable instance
     */
    public static <T> ConstantOrVariable<T> ofVariable(final String variableName) {
        return new ConstantOrVariable<>(null, variableName);
    }

    /**
     * Returns the constant value if this instance represents a constant, null otherwise.
     *
     * @return The constant value or null
     */
    public T getConstant() {
        return constant;
    }

    /**
     * Returns the variable name if this instance represents a variable reference, null otherwise.
     *
     * @return The variable name or null
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * Returns true if this instance represents a constant value.
     *
     * @return true if constant, false if variable
     */
    public boolean isConstant() {
        return constant != null;
    }

    /**
     * Returns true if this instance represents a variable reference.
     *
     * @return true if variable, false if constant
     */
    public boolean isVariable() {
        return variableName != null;
    }

    /**
     * Custom Jackson deserializer for ConstantOrVariable.
     * Handles both direct values and objects with _variableName suffix fields.
     */
    public static class ConstantOrVariableDeserializer extends JsonDeserializer<ConstantOrVariable<?>> {
        @Override
        public ConstantOrVariable<?> deserialize(final JsonParser p, final DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            JsonNode node = p.getCodec().readTree(p);

            // If it's a primitive value (string, number, boolean), treat as constant
            if (node.isValueNode()) {
                Object value = extractPrimitiveValue(node);
                return ConstantOrVariable.ofConstant(value);
            }

            // If it's an object, look for variableName field
            if (node.isObject()) {
                if (node.has("variableName")) {
                    String variableName = node.get("variableName").asText();
                    return ConstantOrVariable.ofVariable(variableName);
                }
            }

            // If we reach here, it's an unexpected format
            throw new JsonProcessingException("Unable to deserialize ConstantOrVariable from: " + node) {};
        }

        private Object extractPrimitiveValue(final JsonNode node) {
            if (node.isTextual()) {
                return node.asText();
            } else if (node.isBoolean()) {
                return node.asBoolean();
            } else if (node.isInt()) {
                return node.asInt();
            } else if (node.isLong()) {
                return node.asLong();
            } else if (node.isDouble()) {
                return node.asDouble();
            } else if (node.isNull()) {
                return null;
            } else {
                throw new IllegalStateException("Should be a primitive json");
            }
        }
    }
}