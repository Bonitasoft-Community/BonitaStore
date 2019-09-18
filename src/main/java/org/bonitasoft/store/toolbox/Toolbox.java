package org.bonitasoft.store.toolbox;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

public class Toolbox {

  /**
   * this is the logger to use in FoodTruck Attention to reduce the usage, and
   * to use foodTruckParam.log, then the log information can be manage at the
   * Input level, as a parameters
   */

  public static SimpleDateFormat sdfJavasscript = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  static Long getLong(final Object parameter, final Long defaultValue) {
    if (parameter == null) {
      return defaultValue;
    }
    try {
      return Long.valueOf(parameter.toString());
    } catch (final Exception e) {
      LoggerStore.logger.severe("Can't decode integer [" + parameter + "]");
      return defaultValue;
    }
  }

  static Integer getInteger(final Object parameter, final Integer defaultValue) {
    if (parameter == null) {
      return defaultValue;
    }
    try {
      return Integer.valueOf(parameter.toString());
    } catch (final Exception e) {
      LoggerStore.logger.severe("Can't decode integer [" + parameter + "]");
      return defaultValue;
    }
  }

  static Boolean getBoolean(final Object parameter, final Boolean defaultValue) {
    if (parameter == null) {
      return defaultValue;
    }
    try {
      return Boolean.valueOf(parameter.toString());
    } catch (final Exception e) {
      LoggerStore.logger.severe("Can't decode boolean [" + parameter + "]");
      return defaultValue;
    }
  }

  static String getString(final Object parameter, final String defaultValue) {
    if (parameter == null) {
      return defaultValue;
    }
    try {
      return parameter.toString();
    } catch (final Exception e) {
      return defaultValue;
    }
  }

  static Date getDate(final Object parameter, final Date defaultValue) {
    if (parameter == null) {
      return defaultValue;
    }
    try {

      return sdfJavasscript.parse(parameter.toString());
    } catch (final Exception e) {
      return defaultValue;
    }
  }

  /**
   * @param parameter
   * @param defaultValue
   * @return
   */
  static List<Map<String, String>> getList(final Object parameter, final List<Map<String, String>> defaultValue) {
    if (parameter == null) {
      return defaultValue;
    }
    try {
      return (List<Map<String, String>>) parameter;
    } catch (final Exception e) {
      return defaultValue;
    }
  }

  /**
   * @param parameter
   * @param defaultValue
   * @return
   */
  static Map<String, Object> getMap(final Object parameter, final Map<String, Object> defaultValue) {
    if (parameter == null) {
      return defaultValue;
    }
    try {
      return (Map<String, Object>) parameter;
    } catch (final Exception e) {
      return defaultValue;
    }
  }

  /**
   * calculate the file name
   *
   * @param directory
   * @param domain
   * @param configFileName
   * @return
   */
  public static String getConfigFileName(final String ldapSynchronizerPath, final String domain, final String configFileName) {
    final String fileName = ldapSynchronizerPath + File.separator + domain + File.separator + configFileName;
    // logger.info("CraneTruck.Toolbox: configuration [" + configFileName +
    // "] file is [" + fileName + "]");
    return fileName;
  }

}
