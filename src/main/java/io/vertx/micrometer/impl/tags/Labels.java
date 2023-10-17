/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package io.vertx.micrometer.impl.tags;

import io.vertx.core.net.SocketAddress;

/**
 * @author Joel Takvorian
 */
public final class Labels {
  private Labels() {
    // Utility
  }

  public static String address(SocketAddress address) {
    return address(address, null);
  }

  public static String address(SocketAddress address, String nameOverride) {
    if (address == null) {
      return "?";
    }
    if (nameOverride == null) {
      return address.toString();
    }
    SocketAddress addrOverride = address.port() >= 0 ? SocketAddress.inetSocketAddress(address.port(), nameOverride)
      : SocketAddress.domainSocketAddress(nameOverride);
    return addrOverride.toString();
  }

  public static String side(boolean local) {
    return local ? "local" : "remote";
  }
}
