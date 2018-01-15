package io.vertx.kotlin.ext.influxdb

import io.vertx.ext.monitoring.influxdb.AuthenticationOptions

/**
 * A function providing a DSL for building [io.vertx.ext.influxdb.AuthenticationOptions] objects.
 *
 * Authentication options.
 *
 * @param enabled  Set whether authentication is enabled. Defaults to <code>false</code>.
 * @param secret  Set the secret used for authentication.
 * @param username  Set the identifier used for authentication.
 *
 * <p/>
 * NOTE: This function has been automatically generated from the [AuthenticationOptions original] using Vert.x codegen.
 */
fun AuthenticationOptions(
  enabled: Boolean? = null,
  secret: String? = null,
  username: String? = null): AuthenticationOptions = AuthenticationOptions().apply {

  if (enabled != null) {
    this.setEnabled(enabled)
  }
  if (secret != null) {
    this.setSecret(secret)
  }
  if (username != null) {
    this.setUsername(username)
  }
}

