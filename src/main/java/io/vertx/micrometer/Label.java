/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.micrometer;

import io.vertx.codegen.annotations.VertxGen;

/**
 * List of labels used in various Vert.x metrics. Labels that may not have bounded values are disabled by default.
 * @author Joel Takvorian
 */
@VertxGen
public enum Label {
  /**
   * Local address in client-host or host-client connections (used in net, http and datagram domains)
   */
  LOCAL("local"),
  /**
   * Remote address in client-host or host-client connections (used in net and http domains)
   */
  REMOTE("remote"),
  /**
   * Path of the URI for client or server requests (used in http domain)
   */
  HTTP_PATH("path"),
  /**
   * Method (GET, POST, PUT, etc.) of an HTTP requests (used in http domain)
   */
  HTTP_METHOD("method"),
  /**
   * HTTP response code (used in http domain)
   */
  HTTP_CODE("code"),
  /**
   * Class name. When used in error counters (in net, http, datagram and eventbus domains) it relates to an exception that occurred.
   * When used in verticle domain, it relates to the verticle class name.
   */
  CLASS_NAME("class"),
  /**
   * Event bus address
   */
  EB_ADDRESS("address"),
  /**
   * Event bus side of the metric, it can be either "local" or "remote"
   */
  EB_SIDE("side"),
  /**
   * Event bus failure name from a ReplyFailure object
   */
  EB_FAILURE("failure"),
  /**
   * Pool type, such as "worker" or "datasource" (used in pools domain)
   */
  POOL_TYPE("pool_type"),
  /**
   * Pool name (used in pools domain)
   */
  POOL_NAME("pool_name");

  private final String labelOutput;

  Label(String labelOutput) {
    this.labelOutput = labelOutput;
  }

  @Override
  public String toString() {
    return labelOutput;
  }
}
