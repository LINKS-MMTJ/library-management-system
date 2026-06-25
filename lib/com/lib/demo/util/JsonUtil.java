package com.lib.demo.util;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    /** 简单 JSON 解析（仅支持字符串值提取和一级对象） */
    @SuppressWarnings("unchecked")
    public static Map<String, String> parseSimple(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || !json.contains("{")) return map;
        json = json.trim();
        json = json.substring(1, json.length() - 1).trim();
        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == ',' || json.charAt(i) == '\n' || json.charAt(i) == '\r')) i++;
            if (i >= json.length()) break;
            // 读取 key
            if (json.charAt(i) == '"') {
                int keyStart = i + 1;
                int keyEnd = json.indexOf('"', keyStart);
                String key = json.substring(keyStart, keyEnd);
                i = keyEnd + 1;
                while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == ':')) i++;
                // 读取 value
                if (i < json.length()) {
                    if (json.charAt(i) == '"') {
                        int valStart = i + 1;
                        int valEnd = json.indexOf('"', valStart);
                        map.put(key, json.substring(valStart, valEnd));
                        i = valEnd + 1;
                    } else if (json.charAt(i) == '{') {
                        int depth = 1, valStart = i;
                        i++;
                        while (i < json.length() && depth > 0) {
                            if (json.charAt(i) == '{') depth++;
                            else if (json.charAt(i) == '}') depth--;
                            i++;
                        }
                        map.put(key, json.substring(valStart, i));
                    } else {
                        int valStart = i;
                        while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
                        map.put(key, json.substring(valStart, i).trim());
                    }
                }
            } else {
                i++;
            }
        }
        return map;
    }

    /** 从 JSON 对象中提取整数字段 */
    public static int getInt(Map<String, String> json, String key, int defaultVal) {
        String v = json.get(key);
        if (v == null) return defaultVal;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return defaultVal; }
    }

    /** 从 JSON 对象中提取 double 字段 */
    public static double getDouble(Map<String, String> json, String key, double defaultVal) {
        String v = json.get(key);
        if (v == null) return defaultVal;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return defaultVal; }
    }

    /** 从 JSON 对象中提取长整型字段 */
    public static Long getLong(Map<String, String> json, String key) {
        String v = json.get(key);
        if (v == null) return null;
        try { return Long.parseLong(v); } catch (NumberFormatException e) { return null; }
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
