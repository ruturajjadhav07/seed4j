package com.seed4j.generator.server.springboot.broker.pulsar.domain;

import static com.seed4j.module.infrastructure.secondary.Seed4JModulesAssertions.*;
import static org.mockito.Mockito.when;

import com.seed4j.TestFileUtils;
import com.seed4j.UnitTest;
import com.seed4j.module.domain.Seed4JModule;
import com.seed4j.module.domain.Seed4JModulesFixture;
import com.seed4j.module.domain.docker.DockerImageVersion;
import com.seed4j.module.domain.docker.DockerImages;
import com.seed4j.module.domain.properties.Seed4JModuleProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PulsarModuleFactoryTest {

  @Mock
  private DockerImages dockerImages;

  @InjectMocks
  private PulsarModuleFactory factory;

  @Test
  void shouldBuildModule() {
    when(dockerImages.get("apachepulsar/pulsar")).thenReturn(new DockerImageVersion("apachepulsar/pulsar", "1.1.1"));

    Seed4JModuleProperties properties = Seed4JModulesFixture.propertiesBuilder(TestFileUtils.tmpDirForTest())
      .basePackage("com.seed4j.growth")
      .build();

    Seed4JModule module = factory.buildModule(properties);

    assertThatModuleWithFiles(module, pomFile(), integrationTestAnnotation(), readmeFile())
      .hasFile("pom.xml")
      .containing(
        """
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-pulsar</artifactId>
            </dependency>
        """
      )
      .containing(
        """
            <dependency>
              <groupId>org.testcontainers</groupId>
              <artifactId>testcontainers-pulsar</artifactId>
              <version>${testcontainers.version}</version>
              <scope>test</scope>
            </dependency>
        """
      )
      .and()
      .hasFile("src/main/docker/pulsar.yml")
      .containing("apachepulsar/pulsar:1.1.1")
      .and()
      .hasFile("src/main/resources/config/application.yml")
      .containing(
        """
        pulsar:
          client:
            service-url: pulsar://localhost:6650
        """
      )
      .and()
      .hasFile("src/test/resources/config/application-test.yml")
      .containing(
        """
        pulsar:
          client:
            num-io-threads: 8
          consumer:
            subscription-name: test-subscription
            topic-names[0]: test-topic
          producer:
            topic-name: test-topic
        """
      )
      .and()
      .hasJavaTests("com/seed4j/growth/PulsarTestContainerExtension.java")
      .hasFile("src/test/java/com/seed4j/growth/IntegrationTest.java")
      .containing("import org.junit.jupiter.api.extension.ExtendWith;")
      .containing("@ExtendWith(PulsarTestContainerExtension.class)")
      .and()
      .hasPrefixedFiles(
        "src/main/java/com/seed4j/growth/wire/pulsar/infrastructure/config",
        "PulsarProperties.java",
        "PulsarConfiguration.java"
      )
      .hasFiles("src/test/java/com/seed4j/growth/wire/pulsar/infrastructure/config/PulsarConfigurationIT.java")
      .hasFile("README.md")
      .containing(
        """
        ```bash
        docker compose -f src/main/docker/pulsar.yml up -d
        ```
        """
      );
  }

  private ModuleFile integrationTestAnnotation() {
    return file("src/test/resources/projects/files/IntegrationTest.java", "src/test/java/com/seed4j/growth/IntegrationTest.java");
  }
}
