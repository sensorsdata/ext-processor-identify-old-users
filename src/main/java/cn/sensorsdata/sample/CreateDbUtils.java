package cn.sensorsdata.sample;

import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by fengjiajie on 16/12/1.
 */
public class CreateDbUtils {
  public static void main(String[] args) throws IOException {
    Options options = new Options();
    options.createIfMissing(true);

    DB db = factory.open(new File(ExtProcessorForIdentifyOldUsers.DB_FILE_PREFIX), options);
    int count = 0;
    try {
      Scanner cin = new Scanner(System.in);
      while (cin.hasNext()) {
        String word = cin.next();
        db.put(bytes(word), bytes(""));
        count++;
      }
    } finally {
      db.close();
    }
    System.out.println("Read count: " + count);
  }
}
