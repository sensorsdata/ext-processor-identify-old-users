package cn.sensorsdata.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Created by fengjiajie on 16/12/6.
 */
public class ExtProcessorForIdentifyOldUsersTest {

  @Test public void testProcess() throws Exception {
    // test/resources/old-user-list-db.tar.gz 的数据库中有一个用户 ID, 为 ThisIsOldUser

    ExtProcessorForIdentifyOldUsers processor = new ExtProcessorForIdentifyOldUsers();
    // 正常数据不受影响
    String data =
        "{\"distinct_id\":\"SomeOne\",\"time\":1434556935000,\"type\":\"track\",\"event\":\"ViewProduct\",\"properties\":{}}";
    String result = processor.process(data);
    assertEquals(data, result);

    // 不在数据库中的数据不受影响
    data =
        "{\"distinct_id\":\"SomeOne\",\"time\":1434556935000,\"type\":\"track\",\"event\":\"$AppStart\",\"properties\":{}}";
    result = processor.process(data);
    assertEquals(data, result);

    // 不在数据库中的数据不受影响
    data =
        "{\"distinct_id\":\"SomeOne\",\"time\":1434556935000,\"type\":\"track\",\"event\":\"$AppStart\",\"properties\":{\"$is_first_time\":true}}";
    result = processor.process(data);
    assertEquals(data, result);

    // 如果在数据库中的老用户, 那么 $AppStart 事件中标记 $is_first_time 为 false
    data =
        "{\"distinct_id\":\"ThisIsOldUser\",\"time\":1434556935000,\"type\":\"track\",\"event\":\"$AppStart\",\"properties\":{\"$is_first_time\":true}}";
    result = processor.process(data);
    assertNotEquals(data, result);
    assertTrue(result.contains("\"$is_first_time\":false"));

    // 正常用户 AppInstall 不受影响
    data =
        "{\"distinct_id\":\"SomeOne\",\"time\":1434556935000,\"type\":\"track\",\"event\":\"AppInstall\",\"properties\":{}}";
    result = processor.process(data);
    assertEquals(data, result);

    // 老用户的 AppInstall 被忽略
    data =
        "{\"distinct_id\":\"ThisIsOldUser\",\"time\":1434556935000,\"type\":\"track\",\"event\":\"AppInstall\",\"properties\":{}}";
    result = processor.process(data);
    assertNull(result);
  }
}