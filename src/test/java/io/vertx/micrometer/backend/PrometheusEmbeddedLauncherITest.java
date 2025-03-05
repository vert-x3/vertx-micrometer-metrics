package io.vertx.micrometer.backend;

import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.MyVerticle;
import io.vertx.micrometer.VertxPrometheusOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.fail;

public class PrometheusEmbeddedLauncherITest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private File output;
  private Process process;

  @Before
  public void setUp() throws Exception {
    output = folder.newFile();
  }

  @After
  public void tearDown() throws Exception {
    if (process != null && process.isAlive()) {
      process.destroyForcibly();
    }
  }

  public static final class ShouldNotFailWithStackOverflowError extends Launcher {
    public static void main(String[] args) {
      ShouldNotFailWithStackOverflowError launcher = new ShouldNotFailWithStackOverflowError();
      launcher.dispatch(new String[]{"run", MyVerticle.class.getName(), "--java-opts", "-Dvertx.metrics.options.enabled=true"});
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
      options.setMetricsOptions(
        (new MicrometerMetricsOptions())
          .setEnabled(true)
          .setPrometheusOptions(
            (new VertxPrometheusOptions())
              .setEnabled(true)
              .setStartEmbeddedServer(true)
              .setEmbeddedServerOptions((new HttpServerOptions()).setPort(8181))));
    }
  }

  @Test
  public void shouldNotFailWithStackOverflowError() throws Exception {
    String javaHome = System.getProperty("java.home");
    String classpath = System.getProperty("java.class.path");

    List<String> command = new ArrayList<>();
    command.add(javaHome + File.separator + "bin" + File.separator + "java");
    command.add("-classpath");
    command.add(classpath);
    command.add(ShouldNotFailWithStackOverflowError.class.getName());

    process = new ProcessBuilder(command)
      .redirectOutput(output)
      .redirectErrorStream(true)
      .start();

    long start = System.nanoTime();
    do {
      MILLISECONDS.sleep(500);
      if (SECONDS.convert(System.nanoTime() - start, NANOSECONDS) > 5) {
        fail("Verticle couldn't be deployed");
      }
    } while (!verticleDeployed());
  }

  private boolean verticleDeployed() throws Exception {
    if (!process.isAlive()) {
      return false;
    }
    return Files.readAllLines(output.toPath()).stream().anyMatch(s -> s.contains("Succeeded in deploying verticle"));
  }
}
