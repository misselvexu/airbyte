/*
 * MIT License
 *
 * Copyright (c) 2020 Airbyte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.airbyte.server.apis;

import io.airbyte.api.model.CheckConnectionRead;
import io.airbyte.api.model.CheckOperationRead;
import io.airbyte.api.model.CompleteDestinationOAuthRequest;
import io.airbyte.api.model.CompleteSourceOauthRequest;
import io.airbyte.api.model.ConnectionCreate;
import io.airbyte.api.model.ConnectionIdRequestBody;
import io.airbyte.api.model.ConnectionRead;
import io.airbyte.api.model.ConnectionReadList;
import io.airbyte.api.model.ConnectionState;
import io.airbyte.api.model.ConnectionUpdate;
import io.airbyte.api.model.DbMigrationExecutionRead;
import io.airbyte.api.model.DbMigrationReadList;
import io.airbyte.api.model.DbMigrationRequestBody;
import io.airbyte.api.model.DestinationCoreConfig;
import io.airbyte.api.model.DestinationCreate;
import io.airbyte.api.model.DestinationDefinitionCreate;
import io.airbyte.api.model.DestinationDefinitionIdRequestBody;
import io.airbyte.api.model.DestinationDefinitionRead;
import io.airbyte.api.model.DestinationDefinitionReadList;
import io.airbyte.api.model.DestinationDefinitionSpecificationRead;
import io.airbyte.api.model.DestinationDefinitionUpdate;
import io.airbyte.api.model.DestinationIdRequestBody;
import io.airbyte.api.model.DestinationOauthConsentRequest;
import io.airbyte.api.model.DestinationRead;
import io.airbyte.api.model.DestinationReadList;
import io.airbyte.api.model.DestinationUpdate;
import io.airbyte.api.model.HealthCheckRead;
import io.airbyte.api.model.ImportRead;
import io.airbyte.api.model.ImportRequestBody;
import io.airbyte.api.model.JobIdRequestBody;
import io.airbyte.api.model.JobInfoRead;
import io.airbyte.api.model.JobListRequestBody;
import io.airbyte.api.model.JobReadList;
import io.airbyte.api.model.LogsRequestBody;
import io.airbyte.api.model.Notification;
import io.airbyte.api.model.NotificationRead;
import io.airbyte.api.model.OAuthConsentRead;
import io.airbyte.api.model.OperationCreate;
import io.airbyte.api.model.OperationIdRequestBody;
import io.airbyte.api.model.OperationRead;
import io.airbyte.api.model.OperationReadList;
import io.airbyte.api.model.OperationUpdate;
import io.airbyte.api.model.OperatorConfiguration;
import io.airbyte.api.model.SlugRequestBody;
import io.airbyte.api.model.SourceCoreConfig;
import io.airbyte.api.model.SourceCreate;
import io.airbyte.api.model.SourceDefinitionCreate;
import io.airbyte.api.model.SourceDefinitionIdRequestBody;
import io.airbyte.api.model.SourceDefinitionRead;
import io.airbyte.api.model.SourceDefinitionReadList;
import io.airbyte.api.model.SourceDefinitionSpecificationRead;
import io.airbyte.api.model.SourceDefinitionUpdate;
import io.airbyte.api.model.SourceDiscoverSchemaRead;
import io.airbyte.api.model.SourceIdRequestBody;
import io.airbyte.api.model.SourceOauthConsentRequest;
import io.airbyte.api.model.SourceRead;
import io.airbyte.api.model.SourceReadList;
import io.airbyte.api.model.SourceUpdate;
import io.airbyte.api.model.UploadRead;
import io.airbyte.api.model.WebBackendConnectionCreate;
import io.airbyte.api.model.WebBackendConnectionRead;
import io.airbyte.api.model.WebBackendConnectionReadList;
import io.airbyte.api.model.WebBackendConnectionRequestBody;
import io.airbyte.api.model.WebBackendConnectionUpdate;
import io.airbyte.api.model.WorkspaceCreate;
import io.airbyte.api.model.WorkspaceIdRequestBody;
import io.airbyte.api.model.WorkspaceRead;
import io.airbyte.api.model.WorkspaceReadList;
import io.airbyte.api.model.WorkspaceUpdate;
import io.airbyte.commons.io.FileTtlManager;
import io.airbyte.config.Configs;
import io.airbyte.config.persistence.ConfigNotFoundException;
import io.airbyte.config.persistence.ConfigRepository;
import io.airbyte.db.Database;
import io.airbyte.scheduler.client.CachingSynchronousSchedulerClient;
import io.airbyte.scheduler.client.SchedulerJobClient;
import io.airbyte.scheduler.persistence.JobNotifier;
import io.airbyte.scheduler.persistence.JobPersistence;
import io.airbyte.scheduler.persistence.WorkspaceHelper;
import io.airbyte.server.converters.SpecFetcher;
import io.airbyte.server.errors.BadObjectSchemaKnownException;
import io.airbyte.server.errors.IdNotFoundKnownException;
import io.airbyte.server.handlers.ArchiveHandler;
import io.airbyte.server.handlers.ConnectionsHandler;
import io.airbyte.server.handlers.DbMigrationHandler;
import io.airbyte.server.handlers.DestinationDefinitionsHandler;
import io.airbyte.server.handlers.DestinationHandler;
import io.airbyte.server.handlers.HealthCheckHandler;
import io.airbyte.server.handlers.JobHistoryHandler;
import io.airbyte.server.handlers.LogsHandler;
import io.airbyte.server.handlers.OAuthHandler;
import io.airbyte.server.handlers.OpenApiConfigHandler;
import io.airbyte.server.handlers.OperationsHandler;
import io.airbyte.server.handlers.SchedulerHandler;
import io.airbyte.server.handlers.SourceDefinitionsHandler;
import io.airbyte.server.handlers.SourceHandler;
import io.airbyte.server.handlers.WebBackendConnectionsHandler;
import io.airbyte.server.handlers.WebBackendDestinationHandler;
import io.airbyte.server.handlers.WebBackendSourceHandler;
import io.airbyte.server.handlers.WorkspacesHandler;
import io.airbyte.server.validators.DockerImageValidator;
import io.airbyte.validation.json.JsonSchemaValidator;
import io.airbyte.validation.json.JsonValidationException;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.io.File;
import java.io.IOException;
import java.util.Map;

@javax.ws.rs.Path("/v1")
public class ConfigurationApi implements io.airbyte.api.V1Api {

  private final WorkspacesHandler workspacesHandler;
  private final SourceDefinitionsHandler sourceDefinitionsHandler;
  private final SourceHandler sourceHandler;
  private final DestinationDefinitionsHandler destinationDefinitionsHandler;
  private final DestinationHandler destinationHandler;
  private final ConnectionsHandler connectionsHandler;
  private final OperationsHandler operationsHandler;
  private final SchedulerHandler schedulerHandler;
  private final JobHistoryHandler jobHistoryHandler;
  private final WebBackendConnectionsHandler webBackendConnectionsHandler;
  private final WebBackendSourceHandler webBackendSourceHandler;
  private final WebBackendDestinationHandler webBackendDestinationHandler;
  private final HealthCheckHandler healthCheckHandler;
  private final ArchiveHandler archiveHandler;
  private final LogsHandler logsHandler;
  private final OpenApiConfigHandler openApiConfigHandler;
  private final DbMigrationHandler dbMigrationHandler;
  private final OAuthHandler oAuthHandler;
  private final Configs configs;

  public ConfigurationApi(final ConfigRepository configRepository,
                          final JobPersistence jobPersistence,
                          final SchedulerJobClient schedulerJobClient,
                          final CachingSynchronousSchedulerClient synchronousSchedulerClient,
                          final Configs configs,
                          final FileTtlManager archiveTtlManager,
                          final WorkflowServiceStubs temporalService,
                          final Database configsDatabase,
                          final Database jobsDatabase) {
    final SpecFetcher specFetcher = new SpecFetcher(synchronousSchedulerClient);
    final JsonSchemaValidator schemaValidator = new JsonSchemaValidator();
    final JobNotifier jobNotifier = new JobNotifier(configs.getWebappUrl(), configRepository, new WorkspaceHelper(configRepository, jobPersistence));
    schedulerHandler = new SchedulerHandler(
        configRepository,
        schedulerJobClient,
        synchronousSchedulerClient,
        jobPersistence,
        configs.getWorkspaceRoot(),
        jobNotifier,
        temporalService);
    final DockerImageValidator dockerImageValidator = new DockerImageValidator(synchronousSchedulerClient);
    final WorkspaceHelper workspaceHelper = new WorkspaceHelper(configRepository, jobPersistence);
    sourceDefinitionsHandler = new SourceDefinitionsHandler(configRepository, dockerImageValidator, synchronousSchedulerClient);
    connectionsHandler = new ConnectionsHandler(configRepository, workspaceHelper);
    operationsHandler = new OperationsHandler(configRepository);
    destinationDefinitionsHandler = new DestinationDefinitionsHandler(configRepository, dockerImageValidator, synchronousSchedulerClient);
    destinationHandler = new DestinationHandler(configRepository, schemaValidator, specFetcher, connectionsHandler);
    sourceHandler = new SourceHandler(configRepository, schemaValidator, specFetcher, connectionsHandler);
    workspacesHandler = new WorkspacesHandler(configRepository, connectionsHandler, destinationHandler, sourceHandler);
    jobHistoryHandler = new JobHistoryHandler(jobPersistence);
    webBackendConnectionsHandler = new WebBackendConnectionsHandler(
        connectionsHandler,
        sourceHandler,
        destinationHandler,
        jobHistoryHandler,
        schedulerHandler,
        operationsHandler);
    oAuthHandler = new OAuthHandler(configRepository);
    webBackendSourceHandler = new WebBackendSourceHandler(sourceHandler, oAuthHandler);
    webBackendDestinationHandler = new WebBackendDestinationHandler(destinationHandler, oAuthHandler);
    healthCheckHandler = new HealthCheckHandler(configRepository);
    archiveHandler = new ArchiveHandler(configs.getAirbyteVersion(), configRepository, jobPersistence, workspaceHelper, archiveTtlManager);
    logsHandler = new LogsHandler();
    openApiConfigHandler = new OpenApiConfigHandler();
    dbMigrationHandler = new DbMigrationHandler(configsDatabase, jobsDatabase);
    this.configs = configs;
  }

  // WORKSPACE

  @Override
  public WorkspaceReadList listWorkspaces() {
    return execute(workspacesHandler::listWorkspaces);
  }

  @Override
  public WorkspaceRead createWorkspace(final WorkspaceCreate workspaceCreate) {
    return execute(() -> workspacesHandler.createWorkspace(workspaceCreate));
  }

  @Override
  public void deleteWorkspace(final WorkspaceIdRequestBody workspaceIdRequestBody) {
    execute(() -> {
      workspacesHandler.deleteWorkspace(workspaceIdRequestBody);
      return null;
    });
  }

  @Override
  public WorkspaceRead getWorkspace(final WorkspaceIdRequestBody workspaceIdRequestBody) {
    return execute(() -> workspacesHandler.getWorkspace(workspaceIdRequestBody));
  }

  @Override
  public WorkspaceRead getWorkspaceBySlug(final SlugRequestBody slugRequestBody) {
    return execute(() -> workspacesHandler.getWorkspaceBySlug(slugRequestBody));
  }

  @Override
  public WorkspaceRead updateWorkspace(final WorkspaceUpdate workspaceUpdate) {
    return execute(() -> workspacesHandler.updateWorkspace(workspaceUpdate));
  }

  @Override
  public NotificationRead tryNotificationConfig(final Notification notification) {
    return execute(() -> workspacesHandler.tryNotification(notification));
  }

  // SOURCE

  @Override
  public SourceDefinitionReadList listSourceDefinitions() {
    return execute(sourceDefinitionsHandler::listSourceDefinitions);
  }

  @Override
  public SourceDefinitionReadList listLatestSourceDefinitions() {
    return execute(sourceDefinitionsHandler::listLatestSourceDefinitions);
  }

  @Override
  public SourceDefinitionRead getSourceDefinition(final SourceDefinitionIdRequestBody sourceDefinitionIdRequestBody) {
    return execute(() -> sourceDefinitionsHandler.getSourceDefinition(sourceDefinitionIdRequestBody));
  }

  @Override
  public SourceDefinitionRead createSourceDefinition(final SourceDefinitionCreate sourceDefinitionCreate) {
    return execute(() -> sourceDefinitionsHandler.createSourceDefinition(sourceDefinitionCreate));
  }

  @Override
  public SourceDefinitionRead updateSourceDefinition(final SourceDefinitionUpdate sourceDefinitionUpdate) {
    return execute(() -> sourceDefinitionsHandler.updateSourceDefinition(sourceDefinitionUpdate));
  }

  // SOURCE SPECIFICATION

  @Override
  public SourceDefinitionSpecificationRead getSourceDefinitionSpecification(final SourceDefinitionIdRequestBody sourceDefinitionIdRequestBody) {
    return execute(() -> schedulerHandler.getSourceDefinitionSpecification(sourceDefinitionIdRequestBody));
  }

  // SOURCE OAUTH

  @Override
  public OAuthConsentRead getSourceOAuthConsent(SourceOauthConsentRequest sourceOauthConsentRequest) {
    return execute(() -> oAuthHandler.getSourceOAuthConsent(sourceOauthConsentRequest));
  }

  @Override
  public Map<String, Object> completeSourceOAuth(CompleteSourceOauthRequest completeSourceOauthRequest) {
    return execute(() -> oAuthHandler.completeSourceOAuth(completeSourceOauthRequest));
  }

  // SOURCE IMPLEMENTATION

  @Override
  public SourceRead createSource(final SourceCreate sourceCreate) {
    return execute(() -> sourceHandler.createSource(sourceCreate));
  }

  @Override
  public SourceRead updateSource(final SourceUpdate sourceUpdate) {
    return execute(() -> sourceHandler.updateSource(sourceUpdate));
  }

  @Override
  public SourceReadList listSourcesForWorkspace(final WorkspaceIdRequestBody workspaceIdRequestBody) {
    return execute(() -> sourceHandler.listSourcesForWorkspace(workspaceIdRequestBody));
  }

  @Override
  public SourceRead getSource(final SourceIdRequestBody sourceIdRequestBody) {
    return execute(() -> sourceHandler.getSource(sourceIdRequestBody));
  }

  @Override
  public void deleteSource(final SourceIdRequestBody sourceIdRequestBody) {
    execute(() -> {
      sourceHandler.deleteSource(sourceIdRequestBody);
      return null;
    });
  }

  @Override
  public CheckConnectionRead checkConnectionToSource(final SourceIdRequestBody sourceIdRequestBody) {
    return execute(() -> schedulerHandler.checkSourceConnectionFromSourceId(sourceIdRequestBody));
  }

  @Override
  public CheckConnectionRead checkConnectionToSourceForUpdate(final SourceUpdate sourceUpdate) {
    return execute(() -> schedulerHandler.checkSourceConnectionFromSourceIdForUpdate(sourceUpdate));
  }

  @Override
  public SourceDiscoverSchemaRead discoverSchemaForSource(final SourceIdRequestBody sourceIdRequestBody) {
    return execute(() -> schedulerHandler.discoverSchemaForSourceFromSourceId(sourceIdRequestBody));
  }

  // DB MIGRATION

  @Override
  public DbMigrationReadList listMigrations(DbMigrationRequestBody request) {
    return execute(() -> dbMigrationHandler.list(request));
  }

  @Override
  public DbMigrationExecutionRead executeMigrations(DbMigrationRequestBody request) {
    return execute(() -> dbMigrationHandler.migrate(request));
  }

  // DESTINATION

  @Override
  public DestinationDefinitionReadList listDestinationDefinitions() {
    return execute(destinationDefinitionsHandler::listDestinationDefinitions);
  }

  @Override
  public DestinationDefinitionReadList listLatestDestinationDefinitions() {
    return execute(destinationDefinitionsHandler::listLatestDestinationDefinitions);
  }

  @Override
  public DestinationDefinitionRead getDestinationDefinition(final DestinationDefinitionIdRequestBody destinationDefinitionIdRequestBody) {
    return execute(() -> destinationDefinitionsHandler.getDestinationDefinition(destinationDefinitionIdRequestBody));
  }

  @Override
  public DestinationDefinitionRead createDestinationDefinition(final DestinationDefinitionCreate destinationDefinitionCreate) {
    return execute(() -> destinationDefinitionsHandler.createDestinationDefinition(destinationDefinitionCreate));
  }

  @Override
  public DestinationDefinitionRead updateDestinationDefinition(final DestinationDefinitionUpdate destinationDefinitionUpdate) {
    return execute(() -> destinationDefinitionsHandler.updateDestinationDefinition(destinationDefinitionUpdate));
  }

  // DESTINATION SPECIFICATION

  @Override
  public DestinationDefinitionSpecificationRead getDestinationDefinitionSpecification(final DestinationDefinitionIdRequestBody destinationDefinitionIdRequestBody) {
    return execute(() -> schedulerHandler.getDestinationSpecification(destinationDefinitionIdRequestBody));
  }

  // DESTINATION OAUTH
  @Override
  public OAuthConsentRead getDestinationOAuthConsent(DestinationOauthConsentRequest destinationOauthConsentRequest) {
    return execute(() -> oAuthHandler.getDestinationOAuthConsent(destinationOauthConsentRequest));
  }

  @Override
  public Map<String, Object> completeDestinationOAuth(CompleteDestinationOAuthRequest requestBody) {
    return execute(() -> oAuthHandler.completeDestinationOAuth(requestBody));
  }

  // DESTINATION IMPLEMENTATION

  @Override
  public DestinationRead createDestination(final DestinationCreate destinationCreate) {
    return execute(() -> destinationHandler.createDestination(destinationCreate));
  }

  @Override
  public void deleteDestination(final DestinationIdRequestBody destinationIdRequestBody) {
    execute(() -> {
      destinationHandler.deleteDestination(destinationIdRequestBody);
      return null;
    });
  }

  @Override
  public DestinationRead updateDestination(final DestinationUpdate destinationUpdate) {
    return execute(() -> destinationHandler.updateDestination(destinationUpdate));
  }

  @Override
  public DestinationReadList listDestinationsForWorkspace(final WorkspaceIdRequestBody workspaceIdRequestBody) {
    return execute(() -> destinationHandler.listDestinationsForWorkspace(workspaceIdRequestBody));
  }

  @Override
  public DestinationRead getDestination(final DestinationIdRequestBody destinationIdRequestBody) {
    return execute(() -> destinationHandler.getDestination(destinationIdRequestBody));
  }

  @Override
  public CheckConnectionRead checkConnectionToDestination(final DestinationIdRequestBody destinationIdRequestBody) {
    return execute(() -> schedulerHandler.checkDestinationConnectionFromDestinationId(destinationIdRequestBody));
  }

  @Override
  public CheckConnectionRead checkConnectionToDestinationForUpdate(final DestinationUpdate destinationUpdate) {
    return execute(() -> schedulerHandler.checkDestinationConnectionFromDestinationIdForUpdate(destinationUpdate));
  }

  // CONNECTION

  @Override
  public ConnectionRead createConnection(final ConnectionCreate connectionCreate) {
    return execute(() -> connectionsHandler.createConnection(connectionCreate));
  }

  @Override
  public ConnectionRead updateConnection(final ConnectionUpdate connectionUpdate) {
    return execute(() -> connectionsHandler.updateConnection(connectionUpdate));
  }

  @Override
  public ConnectionReadList listConnectionsForWorkspace(final WorkspaceIdRequestBody workspaceIdRequestBody) {
    return execute(() -> connectionsHandler.listConnectionsForWorkspace(workspaceIdRequestBody));
  }

  @Override
  public ConnectionRead getConnection(final ConnectionIdRequestBody connectionIdRequestBody) {
    return execute(() -> connectionsHandler.getConnection(connectionIdRequestBody));
  }

  @Override
  public void deleteConnection(final ConnectionIdRequestBody connectionIdRequestBody) {
    execute(() -> {
      operationsHandler.deleteOperationsForConnection(connectionIdRequestBody);
      connectionsHandler.deleteConnection(connectionIdRequestBody);
      return null;
    });
  }

  @Override
  public JobInfoRead syncConnection(final ConnectionIdRequestBody connectionIdRequestBody) {
    return execute(() -> schedulerHandler.syncConnection(connectionIdRequestBody));
  }

  @Override
  public JobInfoRead resetConnection(final ConnectionIdRequestBody connectionIdRequestBody) {
    return execute(() -> schedulerHandler.resetConnection(connectionIdRequestBody));
  }

  // Operations

  @Override
  public CheckOperationRead checkOperation(OperatorConfiguration operatorConfiguration) {
    return execute(() -> operationsHandler.checkOperation(operatorConfiguration));
  }

  @Override
  public OperationRead createOperation(final OperationCreate operationCreate) {
    return execute(() -> operationsHandler.createOperation(operationCreate));
  }

  @Override
  public void deleteOperation(OperationIdRequestBody operationIdRequestBody) {
    execute(() -> {
      operationsHandler.deleteOperation(operationIdRequestBody);
      return null;
    });
  }

  @Override
  public OperationReadList listOperationsForConnection(ConnectionIdRequestBody connectionIdRequestBody) {
    return execute(() -> operationsHandler.listOperationsForConnection(connectionIdRequestBody));
  }

  @Override
  public OperationRead getOperation(OperationIdRequestBody operationIdRequestBody) {
    return execute(() -> operationsHandler.getOperation(operationIdRequestBody));
  }

  @Override
  public OperationRead updateOperation(OperationUpdate operationUpdate) {
    return execute(() -> operationsHandler.updateOperation(operationUpdate));
  }

  @Override
  public ConnectionState getState(final ConnectionIdRequestBody connectionIdRequestBody) {
    return execute(() -> schedulerHandler.getState(connectionIdRequestBody));
  }

  // SCHEDULER
  @Override
  public CheckConnectionRead executeSourceCheckConnection(final SourceCoreConfig sourceConfig) {
    return execute(() -> schedulerHandler.checkSourceConnectionFromSourceCreate(sourceConfig));
  }

  @Override
  public CheckConnectionRead executeDestinationCheckConnection(final DestinationCoreConfig destinationConfig) {
    return execute(() -> schedulerHandler.checkDestinationConnectionFromDestinationCreate(destinationConfig));
  }

  @Override
  public SourceDiscoverSchemaRead executeSourceDiscoverSchema(final SourceCoreConfig sourceCreate) {
    return execute(() -> schedulerHandler.discoverSchemaForSourceFromSourceCreate(sourceCreate));
  }

  @Override
  public JobInfoRead cancelJob(final JobIdRequestBody jobIdRequestBody) {
    return execute(() -> schedulerHandler.cancelJob(jobIdRequestBody));
  }

  // JOB HISTORY

  @Override
  public JobReadList listJobsFor(final JobListRequestBody jobListRequestBody) {
    return execute(() -> jobHistoryHandler.listJobsFor(jobListRequestBody));
  }

  @Override
  public JobInfoRead getJobInfo(final JobIdRequestBody jobIdRequestBody) {
    return execute(() -> jobHistoryHandler.getJobInfo(jobIdRequestBody));
  }

  @Override
  public File getLogs(final LogsRequestBody logsRequestBody) {
    return execute(() -> logsHandler.getLogs(configs, logsRequestBody));
  }

  @Override
  public File getOpenApiSpec() {
    return execute(openApiConfigHandler::getFile);
  }

  // HEALTH
  @Override
  public HealthCheckRead getHealthCheck() {
    return healthCheckHandler.health();
  }

  // WEB BACKEND

  @Override
  public WebBackendConnectionReadList webBackendListConnectionsForWorkspace(final WorkspaceIdRequestBody workspaceIdRequestBody) {
    return execute(() -> webBackendConnectionsHandler.webBackendListConnectionsForWorkspace(workspaceIdRequestBody));
  }

  @Override
  public SourceRead webBackendCreateSource(SourceCreate sourceCreate) {
    return execute(() -> webBackendSourceHandler.webBackendCreateSource(sourceCreate));
  }

  @Override
  public DestinationRead webBackendCreateDestination(DestinationCreate destinationCreate) {
    return execute(() -> webBackendDestinationHandler.webBackendCreateDestination(destinationCreate));
  }

  @Override
  public WebBackendConnectionRead webBackendGetConnection(final WebBackendConnectionRequestBody webBackendConnectionRequestBody) {
    return execute(() -> webBackendConnectionsHandler.webBackendGetConnection(webBackendConnectionRequestBody));
  }

  @Override
  public WebBackendConnectionRead webBackendCreateConnection(WebBackendConnectionCreate webBackendConnectionCreate) {
    return execute(() -> webBackendConnectionsHandler.webBackendCreateConnection(webBackendConnectionCreate));
  }

  @Override
  public WebBackendConnectionRead webBackendUpdateConnection(final WebBackendConnectionUpdate webBackendConnectionUpdate) {
    return execute(() -> webBackendConnectionsHandler.webBackendUpdateConnection(webBackendConnectionUpdate));
  }

  // ARCHIVES

  @Override
  public File exportArchive() {
    return execute(archiveHandler::exportData);
  }

  @Override
  public ImportRead importArchive(final File archiveFile) {
    return execute(() -> archiveHandler.importData(archiveFile));
  }

  @Override
  public File exportWorkspace(WorkspaceIdRequestBody workspaceIdRequestBody) {
    return execute(() -> archiveHandler.exportWorkspace(workspaceIdRequestBody));
  }

  @Override
  public UploadRead uploadArchiveResource(File archiveFile) {
    return execute(() -> archiveHandler.uploadArchiveResource(archiveFile));
  }

  @Override
  public ImportRead importIntoWorkspace(ImportRequestBody importRequestBody) {
    return execute(() -> archiveHandler.importIntoWorkspace(importRequestBody));
  }

  private <T> T execute(HandlerCall<T> call) {
    try {
      return call.call();
    } catch (ConfigNotFoundException e) {
      throw new IdNotFoundKnownException(String.format("Could not find configuration for %s: %s.", e.getType().toString(), e.getConfigId()),
          e.getConfigId(), e);
    } catch (JsonValidationException e) {
      throw new BadObjectSchemaKnownException(
          String.format("The provided configuration does not fulfill the specification. Errors: %s", e.getMessage()), e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private interface HandlerCall<T> {

    T call() throws ConfigNotFoundException, IOException, JsonValidationException;

  }

}
