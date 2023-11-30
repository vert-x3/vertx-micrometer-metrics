/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Options for naming all metrics
 *
 * @author Joel Takvorian
 */
@DataObject
@JsonGen(publicConverter = false)
public class MetricsNaming {
  private String clientQueueTime;
  private String clientQueuePending;
  private String clientProcessingTime;
  private String clientProcessingPending;
  private String clientResetsCount;
  private String datagramBytesRead;
  private String datagramBytesWritten;
  private String datagramErrorCount;
  private String ebHandlers;
  private String ebPending;
  private String ebProcessed;
  private String ebPublished;
  private String ebSent;
  private String ebReceived;
  private String ebDelivered;
  private String ebDiscarded;
  private String ebReplyFailures;
  private String ebBytesRead;
  private String ebBytesWritten;
  private String httpQueueTime;
  private String httpQueuePending;
  private String httpActiveRequests;
  private String httpRequestsCount;
  private String httpRequestBytes;
  private String httpResponseTime;
  private String httpResponsesCount;
  private String httpResponseBytes;
  private String httpActiveWsConnections;
  private String httpRequestResetsCount;
  private String netActiveConnections;
  private String netBytesRead;
  private String netBytesWritten;
  private String netErrorCount;
  private String poolQueueTime;
  private String poolQueuePending;
  private String poolUsage;
  private String poolInUse;
  private String poolUsageRatio;
  private String poolCompleted;

  /**
   * Default constructor
   */
  public MetricsNaming() {
  }

  /**
   * Copy constructor
   *
   * @param other The other {@link MetricsNaming} to copy when creating this
   */
  public MetricsNaming(MetricsNaming other) {
    clientQueueTime = other.clientQueueTime;
    clientQueuePending = other.clientQueuePending;
    clientProcessingTime = other.clientProcessingTime;
    clientProcessingPending = other.clientProcessingPending;
    clientResetsCount = other.clientResetsCount;
    datagramBytesRead = other.datagramBytesRead;
    datagramBytesWritten = other.datagramBytesWritten;
    datagramErrorCount = other.datagramErrorCount;
    ebHandlers = other.ebHandlers;
    ebPending = other.ebPending;
    ebProcessed = other.ebProcessed;
    ebPublished = other.ebPublished;
    ebSent = other.ebSent;
    ebReceived = other.ebReceived;
    ebDelivered = other.ebDelivered;
    ebDiscarded = other.ebDiscarded;
    ebReplyFailures = other.ebReplyFailures;
    ebBytesRead = other.ebBytesRead;
    ebBytesWritten = other.ebBytesWritten;
    httpQueueTime = other.httpQueueTime;
    httpQueuePending = other.httpQueuePending;
    httpActiveRequests = other.httpActiveRequests;
    httpRequestsCount = other.httpRequestsCount;
    httpRequestBytes = other.httpRequestBytes;
    httpResponseTime = other.httpResponseTime;
    httpResponsesCount = other.httpResponsesCount;
    httpResponseBytes = other.httpResponseBytes;
    httpActiveWsConnections = other.httpActiveWsConnections;
    httpRequestResetsCount = other.httpRequestResetsCount;
    netActiveConnections = other.netActiveConnections;
    netBytesRead = other.netBytesRead;
    netBytesWritten = other.netBytesWritten;
    netErrorCount = other.netErrorCount;
    poolQueueTime = other.poolQueueTime;
    poolQueuePending = other.poolQueuePending;
    poolUsage = other.poolUsage;
    poolInUse = other.poolInUse;
    poolUsageRatio = other.poolUsageRatio;
    poolCompleted = other.poolCompleted;
  }

  /**
   * Create an instance from a {@link JsonObject}
   *
   * @param json the JsonObject to create it from
   */
  public MetricsNaming(JsonObject json) {
    this();
    MetricsNamingConverter.fromJson(json, this);
  }

  /**
   * @return a JSON representation of these options
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    MetricsNamingConverter.toJson(this, json);
    return json;
  }

  public static MetricsNaming v3Names() {
    MetricsNaming mn = new MetricsNaming();
    mn.clientQueueTime = "queue.time";
    mn.clientQueuePending = "queue.pending";
    mn.clientProcessingTime = "processing.time";
    mn.clientProcessingPending = "processing.pending";
    mn.clientResetsCount = "reset";
    mn.datagramBytesRead = "bytesReceived";
    mn.datagramBytesWritten = "bytesSent";
    mn.datagramErrorCount = "errors";
    mn.ebHandlers = "handlers";
    mn.ebPending = "pending";
    mn.ebProcessed = "processed";
    mn.ebPublished = "published";
    mn.ebSent = "sent";
    mn.ebReceived = "received";
    mn.ebDelivered = "delivered";
    mn.ebDiscarded = "discarded";
    mn.ebReplyFailures = "replyFailures";
    mn.ebBytesRead = "bytesRead";
    mn.ebBytesWritten = "bytesWritten";
    mn.httpQueueTime = "queue.delay";
    mn.httpQueuePending = "queue.size";
    mn.httpActiveRequests = "requests";
    mn.httpRequestsCount = "requestCount";
    mn.httpRequestBytes = "request.bytes";
    mn.httpResponseTime = "responseTime";
    mn.httpResponsesCount = "responseCount";
    mn.httpResponseBytes = "response.bytes";
    mn.httpActiveWsConnections = "wsConnections";
    mn.httpRequestResetsCount = "requestResetCount";
    mn.netActiveConnections = "connections";
    mn.netBytesRead = "bytesReceived";
    mn.netBytesWritten = "bytesSent";
    mn.netErrorCount = "errors";
    mn.poolQueueTime = "queue.delay";
    mn.poolQueuePending = "queue.size";
    mn.poolUsage = "usage";
    mn.poolInUse = "inUse";
    mn.poolUsageRatio = "ratio";
    mn.poolCompleted = "completed";
    return mn;
  }

  public static MetricsNaming v4Names() {
    MetricsNaming mn = new MetricsNaming();
    mn.clientQueueTime = "queue.time";
    mn.clientQueuePending = "queue.pending";
    mn.clientProcessingTime = "processing.time";
    mn.clientProcessingPending = "processing.pending";
    mn.clientResetsCount = "resets";
    mn.datagramBytesRead = "bytes.read";
    mn.datagramBytesWritten = "bytes.written";
    mn.datagramErrorCount = "errors";
    mn.ebHandlers = "handlers";
    mn.ebPending = "pending";
    mn.ebProcessed = "processed";
    mn.ebPublished = "published";
    mn.ebSent = "sent";
    mn.ebReceived = "received";
    mn.ebDelivered = "delivered";
    mn.ebDiscarded = "discarded";
    mn.ebReplyFailures = "reply.failures";
    mn.ebBytesRead = "bytes.read";
    mn.ebBytesWritten = "bytes.written";
    mn.httpQueueTime = "queue.time";
    mn.httpQueuePending = "queue.pending";
    mn.httpActiveRequests = "active.requests";
    mn.httpRequestsCount = "requests";
    mn.httpRequestBytes = "request.bytes";
    mn.httpResponseTime = "response.time";
    mn.httpResponsesCount = "responses";
    mn.httpResponseBytes = "response.bytes";
    mn.httpActiveWsConnections = "active.ws.connections";
    mn.httpRequestResetsCount = "request.resets";
    mn.netActiveConnections = "active.connections";
    mn.netBytesRead = "bytes.read";
    mn.netBytesWritten = "bytes.written";
    mn.netErrorCount = "errors";
    mn.poolQueueTime = "queue.time";
    mn.poolQueuePending = "queue.pending";
    mn.poolUsage = "usage";
    mn.poolInUse = "in.use";
    mn.poolUsageRatio = "ratio";
    mn.poolCompleted = "completed";
    return mn;
  }

  public String getClientQueueTime() {
    return clientQueueTime;
  }

  public String getClientQueuePending() {
    return clientQueuePending;
  }

  public String getClientProcessingTime() {
    return clientProcessingTime;
  }

  public String getClientProcessingPending() {
    return clientProcessingPending;
  }

  public String getClientResetsCount() {
    return clientResetsCount;
  }

  public String getDatagramBytesRead() {
    return datagramBytesRead;
  }

  public String getDatagramBytesWritten() {
    return datagramBytesWritten;
  }

  public String getDatagramErrorCount() {
    return datagramErrorCount;
  }

  public String getEbHandlers() {
    return ebHandlers;
  }

  public String getEbPending() {
    return ebPending;
  }

  public String getEbProcessed() {
    return ebProcessed;
  }

  public String getEbPublished() {
    return ebPublished;
  }

  public String getEbSent() {
    return ebSent;
  }

  public String getEbReceived() {
    return ebReceived;
  }

  public String getEbDelivered() {
    return ebDelivered;
  }

  public String getEbDiscarded() {
    return ebDiscarded;
  }

  public String getEbReplyFailures() {
    return ebReplyFailures;
  }

  public String getEbBytesRead() {
    return ebBytesRead;
  }

  public String getEbBytesWritten() {
    return ebBytesWritten;
  }

  public String getHttpQueueTime() {
    return httpQueueTime;
  }

  public String getHttpQueuePending() {
    return httpQueuePending;
  }

  public String getHttpActiveRequests() {
    return httpActiveRequests;
  }

  public String getHttpRequestsCount() {
    return httpRequestsCount;
  }

  public String getHttpRequestBytes() {
    return httpRequestBytes;
  }

  public String getHttpResponseTime() {
    return httpResponseTime;
  }

  public String getHttpResponsesCount() {
    return httpResponsesCount;
  }

  public String getHttpResponseBytes() {
    return httpResponseBytes;
  }

  public String getHttpActiveWsConnections() {
    return httpActiveWsConnections;
  }

  public String getHttpRequestResetsCount() {
    return httpRequestResetsCount;
  }

  public String getNetActiveConnections() {
    return netActiveConnections;
  }

  public String getPoolUsage() {
    return poolUsage;
  }

  public String getPoolInUse() {
    return poolInUse;
  }

  public String getPoolUsageRatio() {
    return poolUsageRatio;
  }

  public String getPoolCompleted() {
    return poolCompleted;
  }

  public MetricsNaming setClientQueueTime(String clientQueueTime) {
    this.clientQueueTime = clientQueueTime;
    return this;
  }

  public MetricsNaming setClientQueuePending(String clientQueuePending) {
    this.clientQueuePending = clientQueuePending;
    return this;
  }

  public MetricsNaming setClientProcessingTime(String clientProcessingTime) {
    this.clientProcessingTime = clientProcessingTime;
    return this;
  }

  public MetricsNaming setClientProcessingPending(String clientProcessingPending) {
    this.clientProcessingPending = clientProcessingPending;
    return this;
  }

  public MetricsNaming setClientResetsCount(String clientResetsCount) {
    this.clientResetsCount = clientResetsCount;
    return this;
  }

  public MetricsNaming setDatagramBytesRead(String datagramBytesRead) {
    this.datagramBytesRead = datagramBytesRead;
    return this;
  }

  public MetricsNaming setDatagramBytesWritten(String datagramBytesWritten) {
    this.datagramBytesWritten = datagramBytesWritten;
    return this;
  }

  public MetricsNaming setDatagramErrorCount(String datagramErrorCount) {
    this.datagramErrorCount = datagramErrorCount;
    return this;
  }

  public MetricsNaming setEbHandlers(String ebHandlers) {
    this.ebHandlers = ebHandlers;
    return this;
  }

  public MetricsNaming setEbPending(String ebPending) {
    this.ebPending = ebPending;
    return this;
  }

  public MetricsNaming setEbProcessed(String ebProcessed) {
    this.ebProcessed = ebProcessed;
    return this;
  }

  public MetricsNaming setEbPublished(String ebPublished) {
    this.ebPublished = ebPublished;
    return this;
  }

  public MetricsNaming setEbSent(String ebSent) {
    this.ebSent = ebSent;
    return this;
  }

  public MetricsNaming setEbReceived(String ebReceived) {
    this.ebReceived = ebReceived;
    return this;
  }

  public MetricsNaming setEbDelivered(String ebDelivered) {
    this.ebDelivered = ebDelivered;
    return this;
  }

  public MetricsNaming setEbDiscarded(String ebDiscarded) {
    this.ebDiscarded = ebDiscarded;
    return this;
  }

  public MetricsNaming setEbReplyFailures(String ebReplyFailures) {
    this.ebReplyFailures = ebReplyFailures;
    return this;
  }

  public MetricsNaming setEbBytesRead(String ebBytesRead) {
    this.ebBytesRead = ebBytesRead;
    return this;
  }

  public MetricsNaming setEbBytesWritten(String ebBytesWritten) {
    this.ebBytesWritten = ebBytesWritten;
    return this;
  }

  public MetricsNaming setHttpQueueTime(String httpQueueTime) {
    this.httpQueueTime = httpQueueTime;
    return this;
  }

  public MetricsNaming setHttpQueuePending(String httpQueuePending) {
    this.httpQueuePending = httpQueuePending;
    return this;
  }

  public MetricsNaming setHttpActiveRequests(String httpActiveRequests) {
    this.httpActiveRequests = httpActiveRequests;
    return this;
  }

  public MetricsNaming setHttpRequestsCount(String httpRequestsCount) {
    this.httpRequestsCount = httpRequestsCount;
    return this;
  }

  public MetricsNaming setHttpRequestBytes(String httpRequestBytes) {
    this.httpRequestBytes = httpRequestBytes;
    return this;
  }

  public MetricsNaming setHttpResponseTime(String httpResponseTime) {
    this.httpResponseTime = httpResponseTime;
    return this;
  }

  public MetricsNaming setHttpResponsesCount(String httpResponsesCount) {
    this.httpResponsesCount = httpResponsesCount;
    return this;
  }

  public MetricsNaming setHttpResponseBytes(String httpResponseBytes) {
    this.httpResponseBytes = httpResponseBytes;
    return this;
  }

  public MetricsNaming setHttpActiveWsConnections(String httpActiveWsConnections) {
    this.httpActiveWsConnections = httpActiveWsConnections;
    return this;
  }

  public MetricsNaming setHttpRequestResetsCount(String httpRequestResetsCount) {
    this.httpRequestResetsCount = httpRequestResetsCount;
    return this;
  }

  public MetricsNaming setNetActiveConnections(String netActiveConnections) {
    this.netActiveConnections = netActiveConnections;
    return this;
  }

  public MetricsNaming setPoolUsage(String poolUsage) {
    this.poolUsage = poolUsage;
    return this;
  }

  public MetricsNaming setPoolInUse(String poolInUse) {
    this.poolInUse = poolInUse;
    return this;
  }

  public MetricsNaming setPoolUsageRatio(String poolUsageRatio) {
    this.poolUsageRatio = poolUsageRatio;
    return this;
  }

  public MetricsNaming setPoolCompleted(String poolCompleted) {
    this.poolCompleted = poolCompleted;
    return this;
  }

  public String getNetBytesRead() {
    return netBytesRead;
  }

  public MetricsNaming setNetBytesRead(String netBytesRead) {
    this.netBytesRead = netBytesRead;
    return this;
  }

  public String getNetBytesWritten() {
    return netBytesWritten;
  }

  public MetricsNaming setNetBytesWritten(String netBytesWritten) {
    this.netBytesWritten = netBytesWritten;
    return this;
  }

  public String getNetErrorCount() {
    return netErrorCount;
  }

  public MetricsNaming setNetErrorCount(String netErrorCount) {
    this.netErrorCount = netErrorCount;
    return this;
  }

  public String getPoolQueueTime() {
    return poolQueueTime;
  }

  public MetricsNaming setPoolQueueTime(String poolQueueTime) {
    this.poolQueueTime = poolQueueTime;
    return this;
  }

  public String getPoolQueuePending() {
    return poolQueuePending;
  }

  public MetricsNaming setPoolQueuePending(String poolQueuePending) {
    this.poolQueuePending = poolQueuePending;
    return this;
  }
}
