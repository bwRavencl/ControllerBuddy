package net.brockmatt.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that allows for self-referential resource bundles -- i.e., ResourceBundles
 * with messages that can reference keys from the same bundle using standard JSP-inline ${}
 * notation.  Each instance of the class maintains a simple cache of translated bundles for
 * additional lookup performance.  To avoid potential infinite recursion, the maximum recursion
 * depth is set to 5.
 * <p/>
 * Example:
 *   my.bundle.user.name = Bob Smith
 *   my.bundle.user.state = Maine
 *   my.bundle.greeting = Hello, ${my.bundle.user.name}, I see you're from ${my.bundle.user.state}.
 */
public class ResourceBundleUtil {

  private static final int MAX_RECURSION = 5;

  private Pattern jspPattern = Pattern.compile("\\$\\{([\\w\\.\\-]+)\\}");

  private ConcurrentMap<BundleLocale, ResourceBundle> cache = new ConcurrentHashMap<BundleLocale, ResourceBundle>();

  public ResourceBundle getResourceBundle(String baseName, Locale locale) {
    BundleLocale cachekey = new BundleLocale(baseName, locale);

    // resource bundle already cached?
    ReflectiveResourceBundle bundle = (ReflectiveResourceBundle) cache.get(cachekey);
    if (bundle == null) {
      String key;
      ResourceBundle fetchedBundle = ResourceBundle.getBundle(baseName, locale);
      Map<String, String> messages = new HashMap<String, String>();
      // duplicate the key/value pairs
      Enumeration keys = fetchedBundle.getKeys();
      int i = 0;
      while (keys.hasMoreElements()) {
        key = (String) keys.nextElement();
        messages.put(key, translateMessage(fetchedBundle, key)); // figure out any inline refs before storing
        i++;
      }
      bundle = new ReflectiveResourceBundle(messages);
      cache.put(cachekey, bundle);
    }
    return bundle;
  }

  private String translateMessage(ResourceBundle bundle, String key) {
    return translateMessage(bundle, key, MAX_RECURSION);
  }

  private String translateMessage(ResourceBundle bundle, String key, int iteration) {
    String message = bundle.getString(key);
    if (message != null) {
      StringBuffer sb = new StringBuffer();
      Matcher matcher = jspPattern.matcher(message);
      while (matcher.find() && iteration > 0) {
        // the magic
        matcher.appendReplacement(sb, translateMessage(bundle, matcher.group(1), iteration - 1));
      }
      matcher.appendTail(sb);
      return sb.toString();
    }
    return null;
  }

  // Pretty basic bundle
  private static final class ReflectiveResourceBundle extends ResourceBundle {
    private Map<String, String> messages;
    private Enumeration<String> keys;

    public ReflectiveResourceBundle(Map<String, String> messages) {
      this.messages = messages;
      this.keys = Collections.enumeration(messages.keySet());
    }

    @Override
    public Enumeration<String> getKeys() {
      return keys;
    }

    @Override
    protected Object handleGetObject(String key) {
      return messages.get(key);
    }
  }

  // Key is a combination of Resource baseName + locale
  private static final class BundleLocale {
    private String baseName;
    private Locale locale;

    public BundleLocale(String baseName, Locale locale) {
      this.baseName = baseName;
      this.locale = locale;
    }

    public String getBaseName() {
      return baseName;
    }

    public Locale getLocale() {
      return locale;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof BundleLocale)) return false;
      BundleLocale that = (BundleLocale) o;
      if (baseName != null ? !baseName.equals(that.baseName) : that.baseName != null) return false;
      if (locale != null ? !locale.equals(that.locale) : that.locale != null) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result = baseName != null ? baseName.hashCode() : 0;
      result = 31 * result + (locale != null ? locale.hashCode() : 0);
      return result;
    }
  }

}