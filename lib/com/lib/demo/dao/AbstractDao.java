package com.lib.demo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 泛型 DAO 抽象基类，封装共用的 CRUD 模板方法。
 * 子类只需提供数据文件名和差异化查询方法。
 *
 * @param <T> 实体类型，必须实现 Serializable
 */
abstract class AbstractDao<T extends Serializable> {
    private final String fileName;
    protected Map<Long, T> dataMap = new LinkedHashMap<>();
    protected long nextId = 1;

    protected AbstractDao(String fileName) {
        this.fileName = fileName;
        loadData();
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        Map<Long, T> loaded = FileStore.load(fileName);
        if (loaded != null) {
            this.dataMap = new LinkedHashMap<>(loaded);
            this.nextId = dataMap.keySet().stream().max(Long::compare).orElse(0L) + 1;
        }
    }

    /** 子类提供 ID getter（因为 ID 字段名各实体不同） */
    protected abstract Long getId(T entity);

    /** 子类提供 ID setter */
    protected abstract void setId(T entity, Long id);

    /** 子类提供实体名（用于异常消息） */
    protected abstract String entityName();

    // ─── 通用 CRUD ───

    public synchronized T findById(Long id) {
        return dataMap.get(id);
    }

    public synchronized List<T> findAll() {
        return new ArrayList<>(dataMap.values());
    }

    public synchronized T save(T entity) {
        if (getId(entity) == null) {
            setId(entity, nextId++);
        }
        dataMap.put(getId(entity), entity);
        persist();
        return entity;
    }

    public synchronized T update(T entity) {
        Long id = getId(entity);
        if (id == null || !dataMap.containsKey(id)) {
            throw new IllegalArgumentException(entityName() + "不存在");
        }
        dataMap.put(id, entity);
        persist();
        return entity;
    }

    public synchronized void delete(Long id) {
        dataMap.remove(id);
        persist();
    }

    public synchronized void persist() {
        FileStore.save(fileName, dataMap);
    }
}
