package com.lib.demo.util;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量 JSON 工具（零外部依赖）。
 * 仅支持项目所需的实体类型序列化。
 */
public class JsonUtil {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 对象 → JSON 字符串 */
    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof LocalDate) return "\"" + ((LocalDate) obj).format(DATE_FMT) + "\"";
        if (obj instanceof Enum) return "\"" + ((Enum<?>) obj).name() + "\"";
        if (obj instanceof Map) return mapToJson((Map<?, ?>) obj);
        if (obj instanceof Collection) return listToJson((Collection<?>) obj);
        if (obj.getClass().isArray()) return listToJson(Arrays.asList((Object[]) obj));
        return beanToJson(obj);
    }

    /** 对象 → 紧凑 JSON */
    private static String beanToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        for (Field f : fields) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            if (java.lang.reflect.Modifier.isTransient(f.getModifiers())) continue;
            if ("serialVersionUID".equals(f.getName())) continue;
            try {
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val == null) continue;
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(f.getName()).append("\":").append(toJson(val));
            } catch (Exception ignored) {}
        }
        sb.append("}");
        return sb.toString();
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":").append(toJson(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String listToJson(Collection<?> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append(toJson(item));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // ═══════════════════════════════════════════════════════
    //  JSON 解析器 — 递归下降，支持嵌套对象/数组/转义
    // ═══════════════════════════════════════════════════════

    private static class Parser {
        private final String input;
        private int pos;

        Parser(String input) { this.input = input; this.pos = 0; }

        char peek() { return pos < input.length() ? input.charAt(pos) : '\0'; }
        char next() { return pos < input.length() ? input.charAt(pos++) : '\0'; }

        void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
        }

        /** 解析 JSON 对象，返回 String→String Map */
        Map<String, String> parseObject() {
            Map<String, String> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() != '{') return map;
            next(); // skip '{'
            while (true) {
                skipWhitespace();
                if (peek() == '}') { next(); break; }
                if (peek() == ',') { next(); skipWhitespace(); continue; }
                String key = parseString();
                skipWhitespace();
                if (peek() == ':') next();
                else throw new RuntimeException("Expected ':' at " + pos);
                skipWhitespace();
                String value = parseValue();
                map.put(key, value);
            }
            return map;
        }

        /** 解析字符串字面量（处理转义） */
        String parseString() {
            skipWhitespace();
            if (peek() != '"') throw new RuntimeException("Expected '\"' at " + pos + ", got: " + peek());
            next(); // skip opening '"'
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                char c = next();
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            String hex = input.substring(pos, pos + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                            break;
                        default: sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new RuntimeException("Unterminated string");
        }

        /** 解析值：字符串、数字、对象、数组、布尔、null */
        String parseValue() {
            skipWhitespace();
            char c = peek();
            if (c == '"') return parseString();
            if (c == '{') {
                int start = pos;
                parseObject(); // consume but flatten to string
                return input.substring(start, pos);
            }
            if (c == '[') {
                int start = pos;
                skipArray();
                return input.substring(start, pos);
            }
            // 数字 / 布尔 / null — 读到分隔符
            int start = pos;
            while (pos < input.length() && ",}] \t\n\r".indexOf(input.charAt(pos)) == -1) pos++;
            return input.substring(start, pos).trim();
        }

        void skipArray() {
            if (peek() != '[') return;
            next(); // '['
            int depth = 1;
            while (pos < input.length() && depth > 0) {
                char c = next();
                if (c == '"') { while (pos < input.length() && next() != '"'); }
                else if (c == '[') depth++;
                else if (c == ']') depth--;
            }
        }
    }

    /** 解析 JSON 对象，平铺为 String→String（嵌套值保留原始 JSON 片段） */
    public static Map<String, String> parseSimple(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyMap();
        try {
            return new Parser(json.trim()).parseObject();
        } catch (Exception e) {
            System.err.println("JSON 解析失败: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** 从 JSON Map 中提取 int */
    public static int getInt(Map<String, String> json, String key, int defaultVal) {
        String v = json.get(key);
        if (v == null || v.isEmpty()) return defaultVal;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return defaultVal; }
    }

    /** 从 JSON Map 中提取 long（用于金额·分） */
    public static long getLong(Map<String, String> json, String key, long defaultVal) {
        String v = json.get(key);
        if (v == null || v.isEmpty()) return defaultVal;
        try { return Long.parseLong(v); } catch (NumberFormatException e) { return defaultVal; }
    }

    /** 构建错误响应 JSON */
    public static String errorJson(int code, String message) {
        return "{\"code\":" + code + ",\"message\":\"" + escape(message) + "\"}";
    }

    /** 构建成功响应 JSON（data 为 JSON 字符串） */
    public static String successJson(String dataJson) {
        return "{\"code\":200,\"data\":" + dataJson + "}";
    }

    /** 构建成功响应 JSON（data 为消息） */
    public static String successMsg(String message) {
        return "{\"code\":200,\"data\":{\"message\":\"" + escape(message) + "\"}}";
    }

    /** 构建分页/列表响应 */
    public static String listJson(List<?> list) {
        return successJson(listToJson(list));
    }
}
