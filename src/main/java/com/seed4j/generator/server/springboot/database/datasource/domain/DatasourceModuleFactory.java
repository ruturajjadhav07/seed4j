package com.seed4j.generator.server.springboot.database.datasource.domain;

import static com.seed4j.module.domain.Seed4JModule.artifactId;
import static com.seed4j.module.domain.Seed4JModule.dockerComposeFile;
import static com.seed4j.module.domain.Seed4JModule.documentationTitle;
import static com.seed4j.module.domain.Seed4JModule.from;
import static com.seed4j.module.domain.Seed4JModule.groupId;
import static com.seed4j.module.domain.Seed4JModule.javaDependency;
import static com.seed4j.module.domain.Seed4JModule.lineBeforeText;
import static com.seed4j.module.domain.Seed4JModule.moduleBuilder;
import static com.seed4j.module.domain.Seed4JModule.path;
import static com.seed4j.module.domain.Seed4JModule.propertyKey;
import static com.seed4j.module.domain.Seed4JModule.propertyValue;
import static com.seed4j.module.domain.Seed4JModule.to;
import static com.seed4j.module.domain.Seed4JModule.toSrcMainDocker;
import static com.seed4j.module.domain.Seed4JModule.toSrcTestJava;
import static com.seed4j.module.domain.javadependency.JavaDependencyScope.RUNTIME;

import com.seed4j.module.domain.LogLevel;
import com.seed4j.module.domain.Seed4JModule;
import com.seed4j.module.domain.docker.DockerImageName;
import com.seed4j.module.domain.docker.DockerImageVersion;
import com.seed4j.module.domain.docker.DockerImages;
import com.seed4j.module.domain.file.Seed4JSource;
import com.seed4j.module.domain.properties.Seed4JModuleProperties;
import com.seed4j.shared.error.domain.Assert;
import java.util.function.Consumer;

public class DatasourceModuleFactory {

  private static final String PROPERTIES = "properties";
  private static final String ORG_POSTGRESQL = "org.postgresql";
  private static final String POSTGRESQL = "postgresql";
  private static final String MYSQL = "mysql";
  private static final String MARIADB = "mariadb";

  private static final String TESTCONTAINERS_POSTGRESQL = "testcontainers-postgresql";
  private static final String TESTCONTAINERS_MYSQL = "testcontainers-mysql";
  private static final String TESTCONTAINERS_MARIADB = "testcontainers-mariadb";
  private static final String TESTCONTAINERS_MSSQLSERVER = "testcontainers-mssqlserver";

  private static final String SPRING_DATASOURCE_URL = "spring.datasource.url";
  private static final String SPRING_DATASOURCE_USERNAME = "spring.datasource.username";
  private static final String SPRING_DATASOURCE_PASSWORD = "spring.datasource.password";
  private static final String SPRING_DATASOURCE_DRIVER_CLASS_NAME = "spring.datasource.driver-class-name";

  private static final Seed4JSource SOURCE = from("server/springboot/database/datasource");

  private final DockerImages dockerImages;

  public DatasourceModuleFactory(DockerImages dockerImages) {
    this.dockerImages = dockerImages;
  }

  public Seed4JModule buildPostgreSQL(Seed4JModuleProperties properties) {
    Assert.notNull(PROPERTIES, properties);

    DatasourceProperties datasourceProperties = DatasourceProperties.builder()
      .id(POSTGRESQL)
      .databaseName("PostgreSQL")
      .driverDependency(javaDependency().groupId(ORG_POSTGRESQL).artifactId(POSTGRESQL).scope(RUNTIME).build())
      .driverClassName("org.postgresql.Driver")
      .dockerImageName(new DockerImageName("postgres"))
      .testContainerArtifactId(artifactId(TESTCONTAINERS_POSTGRESQL));

    DockerImageVersion dockerImage = dockerImages.get(datasourceProperties.dockerImageName());

    // @formatter:off
    return moduleBuilder(properties)
      .apply(dockerContainer(dockerImages, datasourceProperties))
      .apply(declareDockerComposeService(datasourceProperties))
      .apply(connectionPool(datasourceProperties))
      .apply(testcontainers(dockerImages, properties, datasourceProperties))
      .springMainProperties()
        .set(propertyKey(SPRING_DATASOURCE_URL), propertyValue("jdbc:postgresql://localhost:5432/" + properties.projectBaseName().name()))
        .set(propertyKey(SPRING_DATASOURCE_USERNAME), propertyValue(properties.projectBaseName().name()))
        .and()
      .springTestProperties()
        .set(
          propertyKey(SPRING_DATASOURCE_URL),
          propertyValue(
            "jdbc:tc:postgresql:" + dockerImage.version().get() + ":///" + properties.projectBaseName().name() + "?TC_TMPFS=/testtmpfs:rw"
          )
        )
        .and()
      .springMainLogger(ORG_POSTGRESQL, LogLevel.WARN)
      .springTestLogger(ORG_POSTGRESQL, LogLevel.WARN)
      .springTestLogger("org.jboss.logging", LogLevel.WARN)
      .build();
    // @formatter:on
  }

  public Seed4JModule buildMariaDB(Seed4JModuleProperties properties) {
    Assert.notNull(PROPERTIES, properties);

    DatasourceProperties datasourceProperties = DatasourceProperties.builder()
      .id(MARIADB)
      .databaseName("MariaDB")
      .driverDependency(javaDependency().groupId("org.mariadb.jdbc").artifactId("mariadb-java-client").scope(RUNTIME).build())
      .driverClassName("org.mariadb.jdbc.Driver")
      .dockerImageName(new DockerImageName(MARIADB))
      .testContainerArtifactId(artifactId(TESTCONTAINERS_MARIADB));

    // @formatter:off
    return moduleBuilder(properties)
      .apply(dockerContainer(dockerImages, datasourceProperties))
      .apply(declareDockerComposeService(datasourceProperties))
      .apply(connectionPool( datasourceProperties))
      .apply(testcontainers(dockerImages,  properties, datasourceProperties))
      .springMainProperties()
        .set(propertyKey(SPRING_DATASOURCE_URL), propertyValue("jdbc:mariadb://localhost:3306/" + properties.projectBaseName().name()))
        .set(propertyKey(SPRING_DATASOURCE_USERNAME), propertyValue("root"))
        .and()
      .build();
    // @formatter:on
  }

  public Seed4JModule buildMySQL(Seed4JModuleProperties properties) {
    Assert.notNull(PROPERTIES, properties);

    DatasourceProperties datasourceProperties = DatasourceProperties.builder()
      .id(MYSQL)
      .databaseName("MySQL")
      .driverDependency(javaDependency().groupId("com.mysql").artifactId("mysql-connector-j").scope(RUNTIME).build())
      .driverClassName("com.mysql.cj.jdbc.Driver")
      .dockerImageName(new DockerImageName(MYSQL))
      .testContainerArtifactId(artifactId(TESTCONTAINERS_MYSQL));

    // @formatter:off
    return moduleBuilder(properties)
      .apply(dockerContainer(dockerImages, datasourceProperties))
      .apply(declareDockerComposeService(datasourceProperties))
      .apply(connectionPool( datasourceProperties))
      .apply(testcontainers(dockerImages,  properties, datasourceProperties))
      .springMainProperties()
        .set(propertyKey(SPRING_DATASOURCE_URL), propertyValue("jdbc:mysql://localhost:3306/" + properties.projectBaseName().name()))
        .set(propertyKey(SPRING_DATASOURCE_USERNAME), propertyValue("root"))
        .and()
      .build();
    // @formatter:on
  }

  public Seed4JModule buildMsSQL(Seed4JModuleProperties properties) {
    Assert.notNull(PROPERTIES, properties);

    DatasourceProperties datasourceProperties = DatasourceProperties.builder()
      .id("mssql")
      .databaseName("MsSQL")
      .driverDependency(javaDependency().groupId("com.microsoft.sqlserver").artifactId("mssql-jdbc").scope(RUNTIME).build())
      .driverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
      .dockerImageName(new DockerImageName("mcr.microsoft.com/mssql/server"))
      .testContainerArtifactId(artifactId(TESTCONTAINERS_MSSQLSERVER));

    // @formatter:off
    return moduleBuilder(properties)
      .apply(dockerContainer(dockerImages, datasourceProperties))
      .apply(declareDockerComposeService(datasourceProperties))
      .apply(connectionPool(datasourceProperties))
      .apply(testcontainers(dockerImages,  properties, datasourceProperties))
      .files()
        .add(SOURCE.append("docker").template("container-license-acceptance.txt"), to("src/test/resources/container-license-acceptance.txt"))
        .add(
          SOURCE.template("MsSQLTestContainerExtension.java"),
          toSrcTestJava().append(properties.basePackage().path()).append("MsSQLTestContainerExtension.java")
        )
        .and()
      .springMainProperties()
        .set(
          propertyKey(SPRING_DATASOURCE_URL),
          propertyValue("jdbc:sqlserver://localhost:1433;database=" + properties.projectBaseName().name() + ";trustServerCertificate=true")
        )
        .set(propertyKey(SPRING_DATASOURCE_USERNAME), propertyValue("SA"))
        .set(propertyKey(SPRING_DATASOURCE_PASSWORD), propertyValue("yourStrong(!)Password"))
        .and()
      .springTestProperties()
        .set(
          propertyKey(SPRING_DATASOURCE_URL),
          propertyValue(
            "jdbc:tc:sqlserver:///;database=" + properties.projectBaseName().name() + ";trustServerCertificate=true?TC_TMPFS=/testtmpfs:rw"
          )
        )
        .set(propertyKey(SPRING_DATASOURCE_USERNAME), propertyValue("SA"))
        .set(propertyKey(SPRING_DATASOURCE_PASSWORD), propertyValue("yourStrong(!)Password"))
        .and()
      .mandatoryReplacements()
        .in(path("src/test/java").append(properties.basePackage().path()).append("IntegrationTest.java"))
          .add(
            lineBeforeText("import org.springframework.boot.test.context.SpringBootTest;"),
            "import org.junit.jupiter.api.extension.ExtendWith;"
          )
          .add(lineBeforeText("public @interface"), "@ExtendWith(MsSQLTestContainerExtension.class)")
          .and()
        .and()
      .springMainLogger("com.microsoft.sqlserver", LogLevel.WARN)
      .springMainLogger("org.reflections", LogLevel.WARN)
      .build();
    // @formatter:on
  }

  public static Consumer<Seed4JModule.Seed4JModuleBuilder> declareDockerComposeService(DatasourceProperties datasourceProperties) {
    return moduleBuilder ->
      moduleBuilder.dockerComposeFile().append(dockerComposeFile("src/main/docker/%s.yml".formatted(datasourceProperties.id())));
  }

  public static Consumer<Seed4JModule.Seed4JModuleBuilder> dockerContainer(
    DockerImages dockerImages,
    DatasourceProperties datasourceProperties
  ) {
    DockerImageVersion dockerImage = dockerImages.get(datasourceProperties.dockerImageName());

    // @formatter:off
    return moduleBuilder ->
      moduleBuilder
        .context()
          .put("srcMainDocker", "src/main/docker")
          .put("databaseType", datasourceProperties.id())
          .put(datasourceProperties.id() + "DockerImageWithVersion", dockerImage.fullName())
          .and()
        .documentation(documentationTitle(datasourceProperties.databaseName()), SOURCE.template("databaseType.md"))
        .startupCommands()
          .dockerCompose("src/main/docker/" + datasourceProperties.id() + ".yml")
          .and()
        .files()
          .add(SOURCE.append("docker").template(datasourceProperties.id() + ".yml"), toSrcMainDocker().append(datasourceProperties.id() + ".yml"));
  }

  public static Consumer<Seed4JModule.Seed4JModuleBuilder> testcontainers(DockerImages dockerImages, Seed4JModuleProperties moduleProperties, DatasourceProperties datasourceProperties) {
    DockerImageVersion dockerImage = dockerImages.get(datasourceProperties.dockerImageName());

    // @formatter:off
    return moduleBuilder ->
      moduleBuilder
        .javaDependencies()
          .addDependency(datasourceProperties.testContainerDependency())
          .and()
        .springTestProperties()
          .set(
            propertyKey(SPRING_DATASOURCE_URL),
            propertyValue("jdbc:tc:" + dockerImage.fullName() + ":///" + moduleProperties.projectBaseName().name())
          )
          .set(propertyKey(SPRING_DATASOURCE_USERNAME), propertyValue(moduleProperties.projectBaseName().name()))
          .set(propertyKey(SPRING_DATASOURCE_PASSWORD), propertyValue(""))
          .set(propertyKey(SPRING_DATASOURCE_DRIVER_CLASS_NAME), propertyValue("org.testcontainers.jdbc.ContainerDatabaseDriver"))
          .and()
        .springTestLogger("com.github.dockerjava", LogLevel.WARN)
        .springTestLogger("org.testcontainers", LogLevel.WARN);
  }

  public static Consumer<Seed4JModule.Seed4JModuleBuilder> connectionPool(DatasourceProperties datasourceProperties) {
    // @formatter:off
    return moduleBuilder ->
      moduleBuilder
        .javaDependencies()
          .addDependency(datasourceProperties.driverDependency())
          .addDependency(groupId("com.zaxxer"), artifactId("HikariCP"))
          .and()
        .springMainProperties()
          .set(propertyKey(SPRING_DATASOURCE_PASSWORD), propertyValue(""))
          .set(propertyKey("spring.datasource.type"), propertyValue("com.zaxxer.hikari.HikariDataSource"))
          .set(propertyKey("spring.datasource.hikari.poolName"), propertyValue("Hikari"))
          .set(propertyKey("spring.datasource.hikari.auto-commit"), propertyValue(false))
          .set(propertyKey(SPRING_DATASOURCE_DRIVER_CLASS_NAME), propertyValue(datasourceProperties.driverClassName()))
          .and()
        .springTestProperties()
          .set(propertyKey("spring.datasource.hikari.maximum-pool-size"), propertyValue(2));
  }
}
