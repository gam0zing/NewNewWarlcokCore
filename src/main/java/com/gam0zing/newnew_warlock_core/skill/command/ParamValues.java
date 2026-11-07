package com.gam0zing.newnew_warlock_core.skill.command;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * 一组类型化参数值，供 {@link PointAllocationCommandType} 的工厂读取。
 * <p>数据来自技能树 JSON 的 {@code params} 对象；缺失的参数取 {@link ParamSpec} 的默认值，
 * 出现未声明的参数一律视为错误，避免数据包里的拼写错误被静默忽略。</p>
 */
public final class ParamValues {

    private final Map<String, Object> values;

    private ParamValues(Map<String, Object> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    /**
     * 按参数声明解析 JSON 参数。
     *
     * @param specs  命令类型声明的参数
     * @param params 节点 JSON 里的 {@code params} 对象，可为 null（全部取默认值）
     * @throws IllegalArgumentException 类型不匹配、存在未声明参数或缺少无默认值的参数
     */
    public static ParamValues parse(List<ParamSpec> specs, JsonObject params) {
        Map<String, Object> parsed = new LinkedHashMap<>();
        if (params != null) {
            for (String key : params.keySet()) {
                boolean declared = specs.stream().anyMatch(spec -> spec.name().equals(key));
                if (!declared) {
                    throw new IllegalArgumentException("undeclared parameter '" + key + "'");
                }
            }
        }
        for (ParamSpec spec : specs) {
            JsonElement element = params == null ? null : params.get(spec.name());
            if (element == null || element.isJsonNull()) {
                if (spec.defaultValue() == null) {
                    throw new IllegalArgumentException("missing required parameter '" + spec.name() + "'");
                }
                parsed.put(spec.name(), spec.defaultValue());
            } else {
                parsed.put(spec.name(), read(spec, element));
            }
        }
        return new ParamValues(parsed);
    }

    private static Object read(ParamSpec spec, JsonElement element) {
        if (!element.isJsonPrimitive()) {
            throw new IllegalArgumentException("parameter '" + spec.name() + "' must be a primitive value");
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        try {
            return switch (spec.type()) {
                case STRING -> {
                    if (!primitive.isString()) {
                        throw new IllegalArgumentException("parameter '" + spec.name() + "' must be a string");
                    }
                    yield primitive.getAsString();
                }
                case INT -> {
                    if (!primitive.isNumber()) {
                        throw new IllegalArgumentException("parameter '" + spec.name() + "' must be an int");
                    }
                    yield primitive.getAsInt();
                }
                case FLOAT -> {
                    if (!primitive.isNumber()) {
                        throw new IllegalArgumentException("parameter '" + spec.name() + "' must be a float");
                    }
                    yield primitive.getAsDouble();
                }
                case BOOL -> {
                    if (!primitive.isBoolean()) {
                        throw new IllegalArgumentException("parameter '" + spec.name() + "' must be a bool");
                    }
                    yield primitive.getAsBoolean();
                }
            };
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "parameter '" + spec.name() + "' is not a valid " + spec.type().id(), exception);
        }
    }

    public String getString(String name) {
        Object value = this.require(name);
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalStateException("parameter '" + name + "' is not a string");
    }

    public int getInt(String name) {
        Object value = this.require(name);
        if (value instanceof Integer integer) {
            return integer;
        }
        throw new IllegalStateException("parameter '" + name + "' is not an int");
    }

    public double getFloat(String name) {
        Object value = this.require(name);
        if (value instanceof Double floating) {
            return floating;
        }
        throw new IllegalStateException("parameter '" + name + "' is not a float");
    }

    public boolean getBool(String name) {
        Object value = this.require(name);
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IllegalStateException("parameter '" + name + "' is not a bool");
    }

    private Object require(String name) {
        if (!this.values.containsKey(name)) {
            throw new IllegalStateException("parameter '" + name + "' is not declared");
        }
        return this.values.get(name);
    }

    /** {@return 原始参数值，未声明时为 null；仅用于展示} */
    @Nullable
    public Object get(String name) {
        return this.values.get(name);
    }

    /** 仅用于定义表导出与调试。 */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        this.values.forEach((key, value) -> {
            if (value instanceof Number number) {
                json.addProperty(key, number);
            } else if (value instanceof Boolean bool) {
                json.addProperty(key, bool);
            } else {
                json.addProperty(key, String.valueOf(value));
            }
        });
        return json;
    }
}
