/*
 * Copyright (c) 2011-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.monitoring.influxdb;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Authentication options.
 *
 * @author Dan Kristensen
 */
@DataObject(generateConverter = true)
public class AuthenticationOptions {
  /**
   * The default value to enable / disable authentication. Disabled by default.
   */
  public static final boolean DEFAULT_ENABLED = false;


  private String username;
  private String secret;
  private boolean enabled;

  public AuthenticationOptions() {
    this.enabled = DEFAULT_ENABLED;
  }

  public AuthenticationOptions(AuthenticationOptions other) {
    username = other.username;
    secret = other.secret;
    enabled = other.enabled;
  }

  public AuthenticationOptions(JsonObject json) {
    AuthenticationOptionsConverter.fromJson(json, this);
  }

  /**
   * @return the identifier used for authentication
   */
  public String getUsername() {
    return username;
  }

  /**
   * Set the identifier used for authentication.
   */
  public AuthenticationOptions setUsername(String username) {
    this.username = username;
    return this;
  }

  /**
   * @return the secret used for authentication
   */
  public String getSecret() {
    return secret;
  }

  /**
   * Set the secret used for authentication.
   */
  public AuthenticationOptions setSecret(String secret) {
    this.secret = secret;
    return this;
  }

  /**
   * @return true if authentication is enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set whether authentication is enabled. Defaults to {@code false}.
   */
  public AuthenticationOptions setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }
}
