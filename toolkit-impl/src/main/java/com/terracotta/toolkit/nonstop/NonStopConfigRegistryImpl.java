/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.nonstop;

import static org.terracotta.toolkit.ToolkitObjectType.CACHE;
import static org.terracotta.toolkit.ToolkitObjectType.LIST;
import static org.terracotta.toolkit.ToolkitObjectType.LOCK;
import static org.terracotta.toolkit.ToolkitObjectType.STORE;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NonStopConfigRegistryImpl implements NonStopConfigurationRegistry {
  public static final EnumSet<ToolkitObjectType>                      SUPPORTED_TOOLKIT_TYPES  = EnumSet.of(STORE,
                                                                                                            CACHE,
                                                                                                            LIST, LOCK);

  public static final NonStopConfiguration                            DEFAULT_CONFIG           = new NonStopConfiguration() {

                                                                                                 @Override
                                                                                                 public long getTimeoutMillis() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_TIMEOUT_MILLIS;
                                                                                                 }

                                                                                                 @Override
                                                                                                 public boolean isEnabled() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_NON_STOP_ENABLED;
                                                                                                 }

                                                                                                 @Override
                                                                                                 public boolean isImmediateTimeoutEnabled() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_NON_STOP_IMMEDIATE_TIMEOUT_ENABLED;
                                                                                                 }

                                                                                                 @Override
                                                                                                 public NonStopConfigurationFields.NonStopReadTimeoutBehavior getReadOpNonStopTimeoutBehavior() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_NON_STOP_READ_TIMEOUT_BEHAVIOR;
                                                                                                 }

                                                                                                 @Override
                                                                                                 public NonStopConfigurationFields.NonStopWriteTimeoutBehavior getWriteOpNonStopTimeoutBehavior() {
                                                                                                   return NonStopConfigurationFields.DEFAULT_NON_STOP_WRITE_TIMEOUT_BEHAVIOR;
                                                                                                 }
                                                                                               };

  private final ThreadLocal<NonStopConfiguration>                     threadLocalConfiguration = new ThreadLocal<NonStopConfiguration>();
  private final ConcurrentMap<NonStopConfigKey, NonStopConfiguration> allConfigs               = new ConcurrentHashMap<NonStopConfigKey, NonStopConfiguration>();

  private void verify(NonStopConfiguration nonStopConfiguration, ToolkitObjectType... types) {
    if (types != null) {
      for (ToolkitObjectType nonStopToolkitTypeParam : types) {
        if (!SUPPORTED_TOOLKIT_TYPES.contains(nonStopToolkitTypeParam)) { throw new UnsupportedOperationException(
                                                                                                                  nonStopToolkitTypeParam
                                                                                                                      .name()
                                                                                                                      + " is not yet supported as a non stop data structure"); }
        if (nonStopToolkitTypeParam != CACHE && nonStopToolkitTypeParam != STORE) {
          if (nonStopConfiguration.getWriteOpNonStopTimeoutBehavior() != NonStopConfigurationFields.NonStopWriteTimeoutBehavior.EXCEPTION) { throw new UnsupportedOperationException(
                                                                                                                                                                                                "behavior "
                                                                                                                                                                                                    + nonStopConfiguration
                                                                                                                                                                                                        .getWriteOpNonStopTimeoutBehavior()
                                                                                                                                                                                                    + " not supported for "
                                                                                                                                                                                                    + nonStopToolkitTypeParam); }

          if (nonStopConfiguration.getReadOpNonStopTimeoutBehavior() != NonStopConfigurationFields.NonStopReadTimeoutBehavior.EXCEPTION) { throw new UnsupportedOperationException(
                                                                                                                                                                                              "behavior "
                                                                                                                                                                                                  + nonStopConfiguration
                                                                                                                                                                                                      .getReadOpNonStopTimeoutBehavior()
                                                                                                                                                                                                  + " not supported for "
                                                                                                                                                                                                  + nonStopToolkitTypeParam); }
        }
      }

    }
  }

  @Override
  public void registerForType(NonStopConfiguration config, ToolkitObjectType... types) {
    verify(config, types);

    for (ToolkitObjectType type : types) {
      allConfigs.put(new NonStopConfigKey(null, type, null), config);
    }
  }

  @Override
  public void registerForInstance(NonStopConfiguration config, String toolkitTypeName, ToolkitObjectType type) {
    verify(config, type);

    allConfigs.put(new NonStopConfigKey(null, type, toolkitTypeName), config);
  }

  @Override
  public void registerForTypeMethod(NonStopConfiguration config, String methodName, ToolkitObjectType type) {
    verify(config, type);

    allConfigs.put(new NonStopConfigKey(methodName, type, null), config);
  }

  @Override
  public void registerForInstanceMethod(NonStopConfiguration config, String methodName, String toolkitTypeName,
                                        ToolkitObjectType type) {
    verify(config, type);

    allConfigs.put(new NonStopConfigKey(methodName, type, toolkitTypeName), config);
  }

  public void registerForThread(NonStopConfiguration config) {
    verify(config);

    threadLocalConfiguration.set(config);
  }

  @Override
  public NonStopConfiguration getConfigForType(ToolkitObjectType type) {
    NonStopConfiguration nonStopConfig = getConfigForThread();
    if (nonStopConfig != null) { return nonStopConfig; }

    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, null);
    nonStopConfig = allConfigs.get(nonStopConfigKey);
    if (nonStopConfig != null) {
      return nonStopConfig;
    } else {
      return DEFAULT_CONFIG;
    }
  }

  @Override
  public NonStopConfiguration getConfigForInstance(String toolkitTypeName, ToolkitObjectType type) {
    NonStopConfiguration nonStopConfig = getConfigForThread();
    if (nonStopConfig != null) { return nonStopConfig; }

    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, toolkitTypeName);
    nonStopConfig = allConfigs.get(nonStopConfigKey);
    if (nonStopConfig == null) {
      nonStopConfig = getConfigForType(type);
    }
    return nonStopConfig;
  }

  @Override
  public NonStopConfiguration getConfigForTypeMethod(String methodName, ToolkitObjectType type) {
    NonStopConfiguration nonStopConfig = getConfigForThread();
    if (nonStopConfig != null) { return nonStopConfig; }

    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, null);
    nonStopConfig = allConfigs.get(nonStopConfigKey);
    if (nonStopConfig == null) {
      nonStopConfig = getConfigForType(type);
    }
    return nonStopConfig;
  }

  @Override
  public NonStopConfiguration getConfigForInstanceMethod(String methodName, String toolkitTypeName,
                                                         ToolkitObjectType type) {
    NonStopConfiguration nonStopConfig = getConfigForThread();
    if (nonStopConfig != null) { return nonStopConfig; }

    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, toolkitTypeName);
    nonStopConfig = allConfigs.get(nonStopConfigKey);

    // try method & type
    if (nonStopConfig == null) {
      nonStopConfigKey = new NonStopConfigKey(methodName, type, null);
      nonStopConfig = allConfigs.get(nonStopConfigKey);
    }

    // try for instance
    if (nonStopConfig == null) {
      nonStopConfig = getConfigForInstance(toolkitTypeName, type);
    }

    return nonStopConfig;
  }

  public NonStopConfiguration getConfigForThread() {
    return threadLocalConfiguration.get();
  }

  @Override
  public NonStopConfiguration deregisterForType(ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, null);
    return allConfigs.remove(nonStopConfigKey);
  }

  @Override
  public NonStopConfiguration deregisterForInstance(String toolkitTypeName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(null, type, toolkitTypeName);
    return allConfigs.remove(nonStopConfigKey);
  }

  @Override
  public NonStopConfiguration deregisterForTypeMethod(String methodName, ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, null);
    return allConfigs.remove(nonStopConfigKey);
  }

  @Override
  public NonStopConfiguration deregisterForInstanceMethod(String methodName, String toolkitTypeName,
                                                          ToolkitObjectType type) {
    NonStopConfigKey nonStopConfigKey = new NonStopConfigKey(methodName, type, toolkitTypeName);
    return allConfigs.remove(nonStopConfigKey);
  }

  public NonStopConfiguration deregisterForThread() {
    NonStopConfiguration old = threadLocalConfiguration.get();
    threadLocalConfiguration.remove();

    return old;
  }

  private static class NonStopConfigKey {
    private final String            method;
    private final String            name;
    private final ToolkitObjectType objectType;

    public NonStopConfigKey(String method, ToolkitObjectType objectType, String name) {
      this.method = method;
      this.name = name;
      this.objectType = objectType;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((method == null) ? 0 : method.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      NonStopConfigKey other = (NonStopConfigKey) obj;
      if (method == null) {
        if (other.method != null) return false;
      } else if (!method.equals(other.method)) return false;
      if (name == null) {
        if (other.name != null) return false;
      } else if (!name.equals(other.name)) return false;
      if (objectType != other.objectType) return false;
      return true;
    }

  }
}