package io.vertx.micrometer;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.micrometer.MetricsNaming}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.micrometer.MetricsNaming} original class using Vert.x codegen.
 */
public class MetricsNamingConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, MetricsNaming obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "clientProcessingPending":
          if (member.getValue() instanceof String) {
            obj.setClientProcessingPending((String)member.getValue());
          }
          break;
        case "clientProcessingTime":
          if (member.getValue() instanceof String) {
            obj.setClientProcessingTime((String)member.getValue());
          }
          break;
        case "clientQueuePending":
          if (member.getValue() instanceof String) {
            obj.setClientQueuePending((String)member.getValue());
          }
          break;
        case "clientQueueTime":
          if (member.getValue() instanceof String) {
            obj.setClientQueueTime((String)member.getValue());
          }
          break;
        case "clientResetsCount":
          if (member.getValue() instanceof String) {
            obj.setClientResetsCount((String)member.getValue());
          }
          break;
        case "datagramBytesRead":
          if (member.getValue() instanceof String) {
            obj.setDatagramBytesRead((String)member.getValue());
          }
          break;
        case "datagramBytesWritten":
          if (member.getValue() instanceof String) {
            obj.setDatagramBytesWritten((String)member.getValue());
          }
          break;
        case "datagramErrorCount":
          if (member.getValue() instanceof String) {
            obj.setDatagramErrorCount((String)member.getValue());
          }
          break;
        case "ebBytesRead":
          if (member.getValue() instanceof String) {
            obj.setEbBytesRead((String)member.getValue());
          }
          break;
        case "ebBytesWritten":
          if (member.getValue() instanceof String) {
            obj.setEbBytesWritten((String)member.getValue());
          }
          break;
        case "ebDelivered":
          if (member.getValue() instanceof String) {
            obj.setEbDelivered((String)member.getValue());
          }
          break;
        case "ebDiscarded":
          if (member.getValue() instanceof String) {
            obj.setEbDiscarded((String)member.getValue());
          }
          break;
        case "ebHandlers":
          if (member.getValue() instanceof String) {
            obj.setEbHandlers((String)member.getValue());
          }
          break;
        case "ebPending":
          if (member.getValue() instanceof String) {
            obj.setEbPending((String)member.getValue());
          }
          break;
        case "ebProcessed":
          if (member.getValue() instanceof String) {
            obj.setEbProcessed((String)member.getValue());
          }
          break;
        case "ebPublished":
          if (member.getValue() instanceof String) {
            obj.setEbPublished((String)member.getValue());
          }
          break;
        case "ebReceived":
          if (member.getValue() instanceof String) {
            obj.setEbReceived((String)member.getValue());
          }
          break;
        case "ebReplyFailures":
          if (member.getValue() instanceof String) {
            obj.setEbReplyFailures((String)member.getValue());
          }
          break;
        case "ebSent":
          if (member.getValue() instanceof String) {
            obj.setEbSent((String)member.getValue());
          }
          break;
        case "httpActiveRequests":
          if (member.getValue() instanceof String) {
            obj.setHttpActiveRequests((String)member.getValue());
          }
          break;
        case "httpActiveWsConnections":
          if (member.getValue() instanceof String) {
            obj.setHttpActiveWsConnections((String)member.getValue());
          }
          break;
        case "httpQueuePending":
          if (member.getValue() instanceof String) {
            obj.setHttpQueuePending((String)member.getValue());
          }
          break;
        case "httpQueueTime":
          if (member.getValue() instanceof String) {
            obj.setHttpQueueTime((String)member.getValue());
          }
          break;
        case "httpRequestBytes":
          if (member.getValue() instanceof String) {
            obj.setHttpRequestBytes((String)member.getValue());
          }
          break;
        case "httpRequestResetsCount":
          if (member.getValue() instanceof String) {
            obj.setHttpRequestResetsCount((String)member.getValue());
          }
          break;
        case "httpRequestsCount":
          if (member.getValue() instanceof String) {
            obj.setHttpRequestsCount((String)member.getValue());
          }
          break;
        case "httpResponseBytes":
          if (member.getValue() instanceof String) {
            obj.setHttpResponseBytes((String)member.getValue());
          }
          break;
        case "httpResponseTime":
          if (member.getValue() instanceof String) {
            obj.setHttpResponseTime((String)member.getValue());
          }
          break;
        case "httpResponsesCount":
          if (member.getValue() instanceof String) {
            obj.setHttpResponsesCount((String)member.getValue());
          }
          break;
        case "netActiveConnections":
          if (member.getValue() instanceof String) {
            obj.setNetActiveConnections((String)member.getValue());
          }
          break;
        case "netBytesRead":
          if (member.getValue() instanceof String) {
            obj.setNetBytesRead((String)member.getValue());
          }
          break;
        case "netBytesWritten":
          if (member.getValue() instanceof String) {
            obj.setNetBytesWritten((String)member.getValue());
          }
          break;
        case "netErrorCount":
          if (member.getValue() instanceof String) {
            obj.setNetErrorCount((String)member.getValue());
          }
          break;
        case "poolCompleted":
          if (member.getValue() instanceof String) {
            obj.setPoolCompleted((String)member.getValue());
          }
          break;
        case "poolInUse":
          if (member.getValue() instanceof String) {
            obj.setPoolInUse((String)member.getValue());
          }
          break;
        case "poolQueuePending":
          if (member.getValue() instanceof String) {
            obj.setPoolQueuePending((String)member.getValue());
          }
          break;
        case "poolQueueTime":
          if (member.getValue() instanceof String) {
            obj.setPoolQueueTime((String)member.getValue());
          }
          break;
        case "poolUsage":
          if (member.getValue() instanceof String) {
            obj.setPoolUsage((String)member.getValue());
          }
          break;
        case "poolUsageRatio":
          if (member.getValue() instanceof String) {
            obj.setPoolUsageRatio((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(MetricsNaming obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(MetricsNaming obj, java.util.Map<String, Object> json) {
    if (obj.getClientProcessingPending() != null) {
      json.put("clientProcessingPending", obj.getClientProcessingPending());
    }
    if (obj.getClientProcessingTime() != null) {
      json.put("clientProcessingTime", obj.getClientProcessingTime());
    }
    if (obj.getClientQueuePending() != null) {
      json.put("clientQueuePending", obj.getClientQueuePending());
    }
    if (obj.getClientQueueTime() != null) {
      json.put("clientQueueTime", obj.getClientQueueTime());
    }
    if (obj.getClientResetsCount() != null) {
      json.put("clientResetsCount", obj.getClientResetsCount());
    }
    if (obj.getDatagramBytesRead() != null) {
      json.put("datagramBytesRead", obj.getDatagramBytesRead());
    }
    if (obj.getDatagramBytesWritten() != null) {
      json.put("datagramBytesWritten", obj.getDatagramBytesWritten());
    }
    if (obj.getDatagramErrorCount() != null) {
      json.put("datagramErrorCount", obj.getDatagramErrorCount());
    }
    if (obj.getEbBytesRead() != null) {
      json.put("ebBytesRead", obj.getEbBytesRead());
    }
    if (obj.getEbBytesWritten() != null) {
      json.put("ebBytesWritten", obj.getEbBytesWritten());
    }
    if (obj.getEbDelivered() != null) {
      json.put("ebDelivered", obj.getEbDelivered());
    }
    if (obj.getEbDiscarded() != null) {
      json.put("ebDiscarded", obj.getEbDiscarded());
    }
    if (obj.getEbHandlers() != null) {
      json.put("ebHandlers", obj.getEbHandlers());
    }
    if (obj.getEbPending() != null) {
      json.put("ebPending", obj.getEbPending());
    }
    if (obj.getEbProcessed() != null) {
      json.put("ebProcessed", obj.getEbProcessed());
    }
    if (obj.getEbPublished() != null) {
      json.put("ebPublished", obj.getEbPublished());
    }
    if (obj.getEbReceived() != null) {
      json.put("ebReceived", obj.getEbReceived());
    }
    if (obj.getEbReplyFailures() != null) {
      json.put("ebReplyFailures", obj.getEbReplyFailures());
    }
    if (obj.getEbSent() != null) {
      json.put("ebSent", obj.getEbSent());
    }
    if (obj.getHttpActiveRequests() != null) {
      json.put("httpActiveRequests", obj.getHttpActiveRequests());
    }
    if (obj.getHttpActiveWsConnections() != null) {
      json.put("httpActiveWsConnections", obj.getHttpActiveWsConnections());
    }
    if (obj.getHttpQueuePending() != null) {
      json.put("httpQueuePending", obj.getHttpQueuePending());
    }
    if (obj.getHttpQueueTime() != null) {
      json.put("httpQueueTime", obj.getHttpQueueTime());
    }
    if (obj.getHttpRequestBytes() != null) {
      json.put("httpRequestBytes", obj.getHttpRequestBytes());
    }
    if (obj.getHttpRequestResetsCount() != null) {
      json.put("httpRequestResetsCount", obj.getHttpRequestResetsCount());
    }
    if (obj.getHttpRequestsCount() != null) {
      json.put("httpRequestsCount", obj.getHttpRequestsCount());
    }
    if (obj.getHttpResponseBytes() != null) {
      json.put("httpResponseBytes", obj.getHttpResponseBytes());
    }
    if (obj.getHttpResponseTime() != null) {
      json.put("httpResponseTime", obj.getHttpResponseTime());
    }
    if (obj.getHttpResponsesCount() != null) {
      json.put("httpResponsesCount", obj.getHttpResponsesCount());
    }
    if (obj.getNetActiveConnections() != null) {
      json.put("netActiveConnections", obj.getNetActiveConnections());
    }
    if (obj.getNetBytesRead() != null) {
      json.put("netBytesRead", obj.getNetBytesRead());
    }
    if (obj.getNetBytesWritten() != null) {
      json.put("netBytesWritten", obj.getNetBytesWritten());
    }
    if (obj.getNetErrorCount() != null) {
      json.put("netErrorCount", obj.getNetErrorCount());
    }
    if (obj.getPoolCompleted() != null) {
      json.put("poolCompleted", obj.getPoolCompleted());
    }
    if (obj.getPoolInUse() != null) {
      json.put("poolInUse", obj.getPoolInUse());
    }
    if (obj.getPoolQueuePending() != null) {
      json.put("poolQueuePending", obj.getPoolQueuePending());
    }
    if (obj.getPoolQueueTime() != null) {
      json.put("poolQueueTime", obj.getPoolQueueTime());
    }
    if (obj.getPoolUsage() != null) {
      json.put("poolUsage", obj.getPoolUsage());
    }
    if (obj.getPoolUsageRatio() != null) {
      json.put("poolUsageRatio", obj.getPoolUsageRatio());
    }
  }
}
