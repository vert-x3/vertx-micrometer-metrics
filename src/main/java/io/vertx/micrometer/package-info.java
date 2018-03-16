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

/**
 * = Vert.x Micrometer Metrics
 *
 * This project is an implementation of the Vert.x Metrics Service Provider Interface (SPI).
 * It uses link:http://micrometer.io/[Micrometer] for managing metrics and reporting to several backends.
 *
 * == Features
 *
 * * Vert.x core tools monitoring: TCP/HTTP client and servers, {@link io.vertx.core.datagram.DatagramSocket},
 * {@link io.vertx.core.eventbus.EventBus} and pools
 * * User defined metrics through Micrometer
 * * Reporting to https://www.influxdata.com/[InfluxDB], https://prometheus.io/[Prometheus] or JMX.
 *
 * == InfluxDB
 *
 * === Prerequisites
 *
 * Follow the https://docs.influxdata.com/influxdb/latest/introduction/getting_started/[instructions to get InfluxDb up and running].
 *
 * === Getting started
 *
 * The modules _${maven.artifactId}_ and _micrometer-registry-influx_ must be present in the classpath.
 *
 * Maven users should add this to their project POM file:
 *
 * [source,xml,subs="+attributes"]
 * ----
 * <dependency>
 *   <groupId>${maven.groupId}</groupId>
 *   <artifactId>${maven.artifactId}</artifactId>
 *   <version>${maven.version}</version>
 * </dependency>
 * <dependency>
 *   <groupId>io.micrometer</groupId>
 *   <artifactId>micrometer-registry-influx</artifactId>
 *   <version>${micrometer.version}</version>
 * </dependency>
 * ----
 *
 * And Gradle users, to their build file:
 *
 * [source,groovy,subs="+attributes"]
 * ----
 * compile '${maven.groupId}:${maven.artifactId}:${maven.version}'
 * compile 'io.micrometer:micrometer-registry-influx:${micrometer.version}'
 * ----
 *
 * === Configuration examples
 *
 * Vert.x does not enable SPI implementations by default. You must enable metric collection in the Vert.x options.
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupMinimalInfluxDB()}
 * ----
 *
 * ==== Using a specific URI and database name
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupInfluxDBWithUriAndDatabase()}
 * ----
 *
 * ==== With authentication
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupInfluxDBWithAuthentication()}
 * ----
 *
 * == Prometheus
 *
 * === Prerequisites
 *
 * Follow the https://prometheus.io/docs/prometheus/latest/getting_started/[instructions to get Prometheus up and running].
 *
 * === Getting started
 *
 * The modules _${maven.artifactId}_ and _micrometer-registry-prometheus_ must be present in the classpath.
 * You may also probably need _vertx-web_, to expose the metrics.
 *
 * Maven users should add this to their project POM file:
 *
 * [source,xml,subs="+attributes"]
 * ----
 * <dependency>
 *   <groupId>${maven.groupId}</groupId>
 *   <artifactId>${maven.artifactId}</artifactId>
 *   <version>${maven.version}</version>
 * </dependency>
 * <dependency>
 *   <groupId>io.micrometer</groupId>
 *   <artifactId>micrometer-registry-prometheus</artifactId>
 *   <version>${micrometer.version}</version>
 * </dependency>
 * <dependency>
 *   <groupId>io.vertx</groupId>
 *   <artifactId>vertx-web</artifactId>
 *   <version>${maven.version}</version>
 * </dependency>
 * ----
 *
 * And Gradle users, to their build file:
 *
 * [source,groovy,subs="+attributes"]
 * ----
 * compile '${maven.groupId}:${maven.artifactId}:${maven.version}'
 * compile 'io.micrometer:micrometer-registry-prometheus:${micrometer.version}'
 * compile 'io.vertx:vertx-web:${maven.version}'
 * ----
 *
 * === Configuration examples
 *
 * Vert.x does not enable SPI implementations by default. You must enable metric collection in the Vert.x options
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupMinimalPrometheus()}
 * ----
 *
 * ==== Using an embedded HTTP server wih custom endpoint
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupPrometheusEmbeddedServer()}
 * ----
 *
 * If the embedded server endpoint is not specified, it defaults to _/metrics_.
 *
 * ==== Binding metrics to an existing Vert.x router
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupPrometheusBoundRouter()}
 * ----
 *
 * == JMX
 *
 * === Getting started
 *
 * The modules _${maven.artifactId}_ and _micrometer-registry-jmx_ must be present in the classpath.
 *
 * Maven users should add this to their project POM file:
 *
 * [source,xml,subs="+attributes"]
 * ----
 * <dependency>
 *   <groupId>${maven.groupId}</groupId>
 *   <artifactId>${maven.artifactId}</artifactId>
 *   <version>${maven.version}</version>
 * </dependency>
 * <dependency>
 *   <groupId>io.micrometer</groupId>
 *   <artifactId>micrometer-registry-jmx</artifactId>
 *   <version>${micrometer.version}</version>
 * </dependency>
 * ----
 *
 * And Gradle users, to their build file:
 *
 * [source,groovy,subs="+attributes"]
 * ----
 * compile '${maven.groupId}:${maven.artifactId}:${maven.version}'
 * compile 'io.micrometer:micrometer-registry-jmx:${micrometer.version}'
 * ----
 *
 * === Configuration examples
 *
 * Vert.x does not enable SPI implementations by default. You must enable metric collection in the Vert.x options
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupMinimalJMX()}
 * ----
 *
 * ==== With step and domain
 *
 * In Micrometer, {@code step} refers to the reporting period, in seconds. {@code domain} is the JMX domain under which
 * MBeans are registered.
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupJMXWithStepAndDomain()}
 * ----
 *
 * == Advanced usage
 *
 * Please refer to {@link io.vertx.micrometer.MicrometerMetricsOptions} for an exhaustive list of options.
 *
 * === Disable some metric domains
 *
 * Restricting the Vert.x modules being monitored can be done using
 * {@link io.vertx.micrometer.MicrometerMetricsOptions#disabledMetricsCategories}.
 *
 * For a full list of domains, see {@link io.vertx.micrometer.MetricsDomain}
 *
 * === User-defined metrics
 *
 * The Micrometer registries are accessible, in order to create new metrics or fetch the existing ones.
 * By default, an unique registry is used and will be shared across the Vert.x instances of the JVM:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#accessDefaultRegistry()}
 * ----
 *
 * It is also possible to have separate registries per Vertx instance, by giving a registry name in metrics options.
 * Then it can be retrieved specifically:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupAndAccessCustomRegistry()}
 * ----
 *
 * As an example, here is a custom timer that will track the execution time of a piece of code that is regularly called:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#customTimerExample()}
 * ----
 *
 * For more examples, documentation about the Micrometer registry and how to create metrics, check
 * link:http://micrometer.io/docs/concepts#_registry[Micrometer doc].
 *
 * === Other instrumentation
 *
 * Since plain access to Micrometer registries is provided, it is possible to leverage the Micrometer API.
 * For instance, to instrument the JVM:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#instrumentJVM()}
 * ----
 *
 * _From link:http://micrometer.io/docs/ref/jvm[Micrometer documentation]._
 *
 * === Label matchers
 *
 * The labels (aka tags, or fields...) can be configured through the use of matchers. Here is an example
 * to whitelist HTTP server metrics per host name and port:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupWithMatcherForFiltering()}
 * ----
 *
 * Matching rules can work on exact strings or regular expressions (the former is more performant).
 * When a pattern matches, the value can also be renamed with an alias. By playing with regex and aliases it is possible
 * to ignore a label partitioning:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#setupWithMatcherForIgnoring()}
 * ----
 *
 * Here, any value for the label "remote" will be replaced with "_".
 *
 * Label matching uses Micrometer's `MeterFilter` under the hood. This API can be accessed directly as well:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#useMicrometerFilters()}
 * ----
 *
 * _See also link:http://micrometer.io/docs/concepts#_meter_filters[other examples]._
 *
 * === Snapshots
 *
 * A {@link io.vertx.micrometer.MetricsService} can be created out of a {@link io.vertx.core.metrics.Measured} object
 * in order to take a snapshot of its related metrics and measurements.
 * The snapshot is returned as a {@link io.vertx.core.json.JsonObject}.
 *
 * A well known _Measured_ object is simply {@link io.vertx.core.Vertx}:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#createFullSnapshot()}
 * ----
 *
 * Other components, such as an {@link io.vertx.core.eventbus.EventBus} or a {@link io.vertx.core.http.HttpServer} are
 * measurable:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#createPartialSnapshot()}
 * ----
 *
 * Finally it is possible to filter the returned metrics from their base names:
 *
 * [source,$lang]
 * ----
 * {@link examples.MetricsExamples#createSnapshotFromPrefix()}
 * ----
 *
 * == Vert.x core tools metrics
 *
 * This section lists all the metrics generated by monitoring the Vert.x core tools.
 *
 * === Net Client
 *
 * [cols="15,50,35", options="header"]
 * |===
 * |Metric type
 * |Metric name
 * |Description
 *
 * |Gauge
 * |{@code vertx_net_client_connections{local=<local address>,remote=<remote address>}}
 * |Number of connections to the remote host currently opened.
 *
 * |Summary
 * |{@code vertx_net_client_bytesReceived{local=<local address>,remote=<remote address>}}
 * |Number of bytes received from the remote host.
 *
 * |Summary
 * |{@code vertx_net_client_bytesSent{local=<local address>,remote=<remote address>}}
 * |Number of bytes sent to the remote host.
 *
 * |Counter
 * |{@code vertx_net_client_errors{local=<local address>,remote=<remote address>,class=<class>}}
 * |Number of errors.
 *
 * |===
 *
 * === HTTP Client
 *
 * [cols="15,50,35", options="header"]
 * |===
 * |Metric type
 * |Metric name
 * |Description
 *
 * |Gauge
 * |{@code vertx_http_client_connections{local=<local address>,remote=<remote address>}}
 * |Number of connections to the remote host currently opened.
 *
 * |Summary
 * |{@code vertx_http_client_bytesReceived{local=<local address>,remote=<remote address>}}
 * |Number of bytes received from the remote host.
 *
 * |Summary
 * |{@code vertx_http_client_bytesSent{local=<local address>,remote=<remote address>}}
 * |Number of bytes sent to the remote host.
 *
 * |Counter
 * |{@code vertx_http_client_errors{local=<local address>,remote=<remote address>,class=<class>}}
 * |Number of errors.
 *
 * |Gauge
 * |{@code vertx_http_client_requests{local=<local address>,remote=<remote address>}}
 * |Number of requests waiting for a response.
 *
 * |Counter
 * |{@code vertx_http_client_requestCount{local=<local address>,remote=<remote address>,method=<http method>}}
 * |Number of requests sent.
 *
 * |Timer
 * |{@code vertx_http_client_responseTime{local=<local address>,remote=<remote address>}}
 * |Response time.
 *
 * |Counter
 * |{@code vertx_http_client_responseCount{local=<local address>,remote=<remote address>,code=<response code>}}
 * |Number of received responses.
 *
 * |Gauge
 * |{@code vertx_http_client_wsConnections{local=<local address>,remote=<remote address>}}
 * |Number of websockets currently opened.
 *
 * |===
 *
 * === Datagram socket
 *
 * [cols="15,50,35", options="header"]
 * |===
 * |Metric type
 * |Metric name
 * |Description
 *
 * |Summary
 * |{@code vertx_datagram_bytesReceived{local=<local>,remote=<remote>}}
 * |Total number of bytes received on the {@code <host>:<port>} listening address.
 *
 * |Summary
 * |{@code vertx_datagram_bytesSent{remote=<remote>}}
 * |Total number of bytes sent to the remote host.
 *
 * |Counter
 * |{@code vertx_datagram_errors{class=<class>}}
 * |Total number of errors.
 *
 * |===
 *
 * === Net Server
 *
 * [cols="15,50,35", options="header"]
 * |===
 * |Metric type
 * |Metric name
 * |Description
 *
 * |Gauge
 * |{@code vertx_net_server_connections{local=<local address>}}
 * |Number of opened connections to the Net Server.
 *
 * |Summary
 * |{@code vertx_net_server_bytesReceived{local=<local address>}}
 * |Number of bytes received by the Net Server.
 *
 * |Summary
 * |{@code vertx_net_server_bytesSent{local=<local address>}}
 * |Number of bytes sent by the Net Server.
 *
 * |Counter
 * |{@code vertx_net_server_errors{local=<local address>,class=<class>}}
 * |Number of errors.
 *
 * |===
 *
 * === HTTP Server
 *
 * [cols="15,50,35", options="header"]
 * |===
 * |Metric type
 * |Metric name
 * |Description
 *
 * |Gauge
 * |{@code vertx_http_server_connections{local=<local address>}}
 * |Number of opened connections to the HTTP Server.
 *
 * |Summary
 * |{@code vertx_http_server_bytesReceived{local=<local address>}}
 * |Number of bytes received by the HTTP Server.
 *
 * |Summary
 * |{@code vertx_http_server_bytesSent{local=<local address>}}
 * |Number of bytes sent by the HTTP Server.
 *
 * |Counter
 * |{@code vertx_http_server_errors{local=<local address>,class=<class>}}
 * |Number of errors.
 *
 * |Gauge
 * |{@code vertx_http_server_requests{local=<local address>}}
 * |Number of requests being processed.
 *
 * |Counter
 * |{@code vertx_http_server_requestCount{local=<local address>,method=<http method>,code=<response code>}}
 * |Number of processed requests.
 *
 * |Counter
 * |{@code vertx_http_server_requestResetCount{local=<local address>}}
 * |Number of requests reset.
 *
 * |Timer
 * |{@code vertx_http_server_processingTime{local=<local address>}}
 * |Request processing time.
 *
 * |Gauge
 * |{@code vertx_http_client_wsConnections{local=<local address>}}
 * |Number of websockets currently opened.
 *
 * |===
 *
 * === Event Bus
 *
 * [cols="15,50,35", options="header"]
 * |===
 * |Metric type
 * |Metric name
 * |Description
 *
 * |Gauge
 * |{@code vertx_eventbus_handlers{address=<address>}}
 * |Number of event bus handlers in use.
 *
 * |Counter
 * |{@code vertx_eventbus_errors{address=<address>,class=<class>}}
 * |Number of errors.
 *
 * |Summary
 * |{@code vertx_eventbus_bytesWritten{address=<address>}}
 * |Total number of bytes sent while sending messages to event bus cluster peers.
 *
 * |Summary
 * |{@code vertx_eventbus_bytesRead{address=<address>}}
 * |Total number of bytes received while reading messages from event bus cluster peers.
 *
 * |Gauge
 * |{@code vertx_eventbus_pending{address=<address>,side=<local/remote>}}
 * |Number of messages not processed yet. One message published will count for {@code N} pending if {@code N} handlers
 * are registered to the corresponding address.
 *
 * |Counter
 * |{@code vertx_eventbus_published{address=<address>,side=<local/remote>}}
 * |Number of messages published (publish / subscribe).
 *
 * |Counter
 * |{@code vertx_eventbus_sent{address=<address>,side=<local/remote>}}
 * |Number of messages sent (point-to-point).
 *
 * |Counter
 * |{@code vertx_eventbus_received{address=<address>,side=<local/remote>}}
 * |Number of messages received.
 *
 * |Counter
 * |{@code vertx_eventbus_delivered{address=<address>,side=<local/remote>}}
 * |Number of messages delivered to handlers.
 *
 * |Counter
 * |{@code vertx_eventbus_replyFailures{address=<address>,failure=<failure name>}}
 * |Number of message reply failures.
 *
 * |Timer
 * |{@code vertx_eventbus_processingTime{address=<address>}}
 * |Processing time for handlers listening to the {@code address}.
 *
 * |===
 *
 * == Vert.x pool metrics
 *
 * This section lists all the metrics generated by monitoring Vert.x pools.
 *
 * There are two types currently supported:
 *
 * * _worker_ (see {@link io.vertx.core.WorkerExecutor})
 * * _datasource_ (created with Vert.x JDBC client)
 *
 * NOTE: Vert.x creates two worker pools upfront, _worker-thread_ and _internal-blocking_.
 *
 * [cols="15,50,35", options="header"]
 * |===
 * |Metric type
 * |Metric name
 * |Description
 *
 * |Timer
 * |{@code vertx_pool_queue_delay{pool_type=<type>,pool_name=<name>}}
 * |Time waiting for a resource (queue time).
 *
 * |Gauge
 * |{@code vertx_pool_queue_size{pool_type=<type>,pool_name=<name>}}
 * |Number of elements waiting for a resource.
 *
 * |Timer
 * |{@code vertx_pool_usage{pool_type=<type>,pool_name=<name>}}
 * |Time using a resource (i.e. processing time for worker pools).
 *
 * |Gauge
 * |{@code vertx_pool_inUse{pool_type=<type>,pool_name=<name>}}
 * |Number of resources used.
 *
 * |Counter
 * |{@code vertx_pool_completed{pool_type=<type>,pool_name=<name>}}
 * |Number of elements done with the resource (i.e. total number of tasks executed for worker pools).
 *
 * |Gauge
 * |{@code vertx_pool_ratio{pool_type=<type>,pool_name=<name>}}
 * |Pool usage ratio, only present if maximum pool size could be determined.
 *
 * |===
 *
 * == Verticle metrics
 *
 * [cols="15,50,35", options="header"]
 * |===
 * |Metric type
 * |Metric name
 * |Description
 *
 * |Gauge
 * |{@code vertx_verticle_deployed{name=<name>}}
 * |Number of verticle instances deployed.
 *
 * |===
 *
 */
@ModuleGen(name = "vertx-micrometer-metrics", groupPackage = "io.vertx")
@Document(fileName = "index.adoc")
package io.vertx.micrometer;

import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.docgen.Document;
