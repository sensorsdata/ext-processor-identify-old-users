package cn.sensorsdata.sample;

import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

import com.sensorsdata.analytics.extractor.processor.ExtProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by fengjiajie on 16/12/1.
 */
public class ExtProcessorForIdentifyOldUsers implements ExtProcessor {

  /**
   * 客户端 SDK 调用 trackInstallation 时指定的事件名, 请修改为实际使用的事件名
   */
  private static final String TRACK_INSTALLATION_EVENT_NAME = "AppInstall";

  public static final String DB_FILE_PREFIX = "old-user-list-db";
  private static ObjectMapper objectMapper = new ObjectMapper();
  private static DB levelDb;

  public String process(String record) throws Exception {
    JsonNode recordNode = objectMapper.readTree(record);

    JsonNode typeNode = recordNode.get("type");
    if (typeNode != null && "track".equals(typeNode.asText())) {
      String eventName = recordNode.get("event").asText();
      // 该 ExtProcessor 仅处理 $AppStart 和 AppInstall 两个事件
      if ("$AppStart".equals(eventName) || TRACK_INSTALLATION_EVENT_NAME.equals(eventName)) {
        String distinctId = recordNode.get("distinct_id").asText();
        if (levelDb.get(bytes(distinctId)) != null) {
          // 如果可以查到说明是老用户
          if ("$AppStart".equals(eventName)) {
            // 需要修改 $is_first_time 字段
            ObjectNode propertiesNode = (ObjectNode) recordNode.get("properties");
            JsonNode isFirstTimeNode = propertiesNode.get("$is_first_time");
            if (isFirstTimeNode != null && isFirstTimeNode.asBoolean()) {
              // 需要修改字段为 false
              propertiesNode.put("$is_first_time", false);
            }
          } else if (TRACK_INSTALLATION_EVENT_NAME.equals(eventName)) {
            // 需要忽略, 因为是老用户, 不是首次安装
            return null;
          }

        }
      }
    }

    return recordNode.toString();
  }

  public ExtProcessorForIdentifyOldUsers() throws Exception {
    if (levelDb == null) {
      synchronized (ExtProcessorForIdentifyOldUsers.class) {
        if (levelDb == null) {
          File dbDir;
          try {
            dbDir = unArchiveDb();
          } catch (IOException e) {
            e.printStackTrace();
            throw new Exception("Can't unarchive db", e);
          }

          try {
            initDb(dbDir);
          } catch (IOException e) {
            e.printStackTrace();
            throw new Exception("Init db failed", e);
          }
        }
      }
    }
  }

  private static File unArchiveDb() throws IOException {
    final File tempDir = File.createTempFile(DB_FILE_PREFIX, Long.toString(System.nanoTime()));
    if (!tempDir.delete()) {
      throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath());
    }
    if (!tempDir.mkdir()) {
      throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath());
    }
    tempDir.deleteOnExit();

    try (TarArchiveInputStream ais = new TarArchiveInputStream(new GzipCompressorInputStream(
        ExtProcessorForIdentifyOldUsers.class.getClassLoader()
            .getResourceAsStream(DB_FILE_PREFIX + ".tar.gz")))) {
      TarArchiveEntry entry;
      while ((entry = ais.getNextTarEntry()) != null) {
        if (entry.isFile()) {
          String name = entry.getName();
          File file = new File(tempDir, name);
          file.getParentFile().mkdirs();
          file.getParentFile().deleteOnExit();
          try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            IOUtils.copy(ais, os);
          }
          file.deleteOnExit();
        }
      }
    }
    return new File(tempDir, DB_FILE_PREFIX);
  }

  private static void initDb(File dbDir) throws IOException {
    Options options = new Options();
    options.createIfMissing(false);
    levelDb = factory.open(dbDir, options);
  }
}
