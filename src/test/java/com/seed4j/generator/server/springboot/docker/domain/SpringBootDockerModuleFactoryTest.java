package com.seed4j.generator.server.springboot.docker.domain;

import static com.seed4j.module.infrastructure.secondary.Seed4JModulesAssertions.*;

import com.seed4j.TestFileUtils;
import com.seed4j.UnitTest;
import com.seed4j.module.domain.Seed4JModule;
import com.seed4j.module.domain.Seed4JModulesFixture;
import com.seed4j.module.domain.properties.Seed4JModuleProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class SpringBootDockerModuleFactoryTest {

  private static final SpringBootDockerModuleFactory factory = new SpringBootDockerModuleFactory();

  @Nested
  class Maven {

    @Test
    void shouldBuildJibModule() {
      Seed4JModuleProperties properties = Seed4JModulesFixture.propertiesBuilder(TestFileUtils.tmpDirForTest())
        .basePackage("com.seed4j.growth")
        .projectBaseName("myapp")
        .put("serverPort", 9000)
        .build();

      Seed4JModule module = factory.buildJibModule(properties);

      assertThatModuleWithFiles(module, pomFile())
        .hasFile("pom.xml")
        .containing(
          """
                <plugin>
                  <groupId>com.google.cloud.tools</groupId>
                  <artifactId>jib-maven-plugin</artifactId>
                  <version>${jib-maven-plugin.version}</version>
                  <configuration>
                    <from>
                      <image>eclipse-temurin:25-jre-jammy</image>
                      <platforms>
                        <platform>
                          <architecture>amd64</architecture>
                          <os>linux</os>
                        </platform>
                      </platforms>
                    </from>
                    <to>
                      <image>myapp:latest</image>
                    </to>
                    <container>
                      <entrypoint>
                        <shell>bash</shell>
                        <option>-c</option>
                        <arg>/entrypoint.sh</arg>
                      </entrypoint>
                      <ports>
                        <port>9000</port>
                      </ports>
                      <environment>
                        <SPRING_OUTPUT_ANSI_ENABLED>ALWAYS</SPRING_OUTPUT_ANSI_ENABLED>
                        <SEED4J_SLEEP>0</SEED4J_SLEEP>
                      </environment>
                      <creationTime>USE_CURRENT_TIMESTAMP</creationTime>
                      <user>1000</user>
                    </container>
                    <extraDirectories>
                      <paths>src/main/docker/jib</paths>
                      <permissions>
                        <permission>
                          <file>/entrypoint.sh</file>
                          <mode>755</mode>
                        </permission>
                      </permissions>
                    </extraDirectories>
                  </configuration>
                </plugin>
          """
        )
        .and()
        .hasFile("src/main/docker/jib/entrypoint.sh")
        .containing("\"com.seed4j.growth.MyappApp\"");
    }

    @Test
    void shouldBuildDockerFileModule() {
      Seed4JModuleProperties properties = Seed4JModulesFixture.propertiesBuilder(TestFileUtils.tmpDirForTest())
        .put("serverPort", 9000)
        .build();

      Seed4JModule module = factory.buildDockerFileMavenModule(properties);

      assertThatModule(module).hasFile("Dockerfile").containing("EXPOSE 9000").containing("./mvnw");
    }
  }

  @Nested
  class Gradle {

    @Test
    void shouldBuildJibModule() {
      Seed4JModuleProperties properties = Seed4JModulesFixture.propertiesBuilder(TestFileUtils.tmpDirForTest())
        .basePackage("com.seed4j.growth")
        .projectBaseName("myapp")
        .put("serverPort", 9000)
        .build();

      Seed4JModule module = factory.buildJibModule(properties);

      assertThatModuleWithFiles(module, gradleBuildFile(), gradleLibsVersionFile())
        .hasFile("gradle/libs.versions.toml")
        .containing(
          """
          [versions]
          \tjib = "\
          """
        )
        .containing(
          """
          \t[plugins.jib]
          \t\tid = "com.google.cloud.tools.jib"

          \t\t[plugins.jib.version]
          \t\t\tref = "jib"
          """
        )
        .and()
        .hasFile("build.gradle.kts")
        .containing(
          """
            alias(libs.plugins.jib)
            // seed4j-needle-gradle-plugins
          """
        )
        .containing(
          """
          jib {
            from {
              image = "eclipse-temurin:25-jre-jammy"
              platforms {
                platform {
                  architecture = "amd64"
                  os = "linux"
                }
              }
            }
            to {
              image = "myapp:latest"
            }
            container {
              entrypoint = listOf("bash", "-c", "/entrypoint.sh")
              ports = listOf("9000")
              environment = mapOf(
               "SPRING_OUTPUT_ANSI_ENABLED" to "ALWAYS",
               "SEED4J_SLEEP" to "0"
              )
              creationTime = "USE_CURRENT_TIMESTAMP"
              user = "1000"
            }
            extraDirectories {
              paths {
                path {
                  setFrom("src/main/docker/jib")
                }
              }
              permissions = mapOf("/entrypoint.sh" to "755")
            }
          }
          """
        )
        .and()
        .hasFile("src/main/docker/jib/entrypoint.sh")
        .containing("\"com.seed4j.growth.MyappApp\"");
    }

    @Test
    void shouldBuildDockerFileModule() {
      Seed4JModuleProperties properties = Seed4JModulesFixture.propertiesBuilder(TestFileUtils.tmpDirForTest())
        .put("serverPort", 9000)
        .build();

      Seed4JModule module = factory.buildDockerFileGradleModule(properties);

      assertThatModule(module).hasFile("Dockerfile").containing("EXPOSE 9000").containing("./gradlew");
    }
  }

  @Nested
  class SpringBootDockerComposeModuleTest {

    @Test
    void shouldBuildSpringBootDockerComposeIntegrationModule() {
      Seed4JModuleProperties properties = Seed4JModulesFixture.propertiesBuilder(TestFileUtils.tmpDirForTest())
        .put("serverPort", 9000)
        .build();

      Seed4JModule module = factory.buildSpringBootDockerComposeModule(properties);
      assertThatModuleWithFiles(module, pomFile(), logbackFile(), testLogbackFile(), readmeFile())
        .hasFile("pom.xml")
        .containing(
          """
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-docker-compose</artifactId>
                <scope>runtime</scope>
                <optional>true</optional>
              </dependency>
          """
        )
        .and()
        .hasFile("src/test/resources/config/application-test.yml")
        .containing(
          """
          spring:
            docker:
              compose:
                enabled: false
          """
        );
    }
  }
}
