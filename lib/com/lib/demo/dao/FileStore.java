package com.lib.demo.dao;

import com.lib.demo.exception.DataAccessException;
import com.lib.demo.util.LogUtil;

import java.io.*;
import java.nio.file.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 文件持久化存储引擎。
 * 使用 Java 序列化 + 原子写（写临时文件后 rename）保证数据安全。
 */
class FileStore {
    private static final Logger LOG = LogUtil.getLogger(FileStore.class);
    static final String DATA_DIR = "library_data";

    static {
        ensureDataDir();
    }

    static void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    static String getFilePath(String fileName) {
        return DATA_DIR + File.separator + fileName;
    }

    @SuppressWarnings("unchecked")
    static <T> T load(String fileName) {
        String path = getFilePath(fileName);
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "加载数据文件失败 [" + fileName + "]: " + e.getMessage(), e);
            throw new DataAccessException("加载数据文件失败: " + fileName, e);
        }
    }

    static <T> void save(String fileName, T data) {
        ensureDataDir();
        String path = getFilePath(fileName);
        Path dest = Paths.get(path);
        Path tmp = Paths.get(path + ".tmp");
        // 先写临时文件
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmp.toFile()))) {
            oos.writeObject(data);
            oos.flush();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "保存数据文件失败 [" + fileName + "]: " + e.getMessage(), e);
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            throw new DataAccessException("保存数据文件失败: " + fileName, e);
        }
        // 原子移动替换原文件
        try {
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "原子移动失败 [" + fileName + "]: " + e.getMessage() + "，回退到非原子替换");
            try {
                Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e2) {
                LOG.log(Level.SEVERE, "非原子替换也失败 [" + fileName + "]: " + e2.getMessage(), e2);
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
                throw new DataAccessException("保存数据文件失败(移动): " + fileName, e2);
            }
        }
    }
}
