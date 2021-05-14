package org.camunda.bpm.scenario.report.bpmn;

import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.scenario.report.Report;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * @author Martin Schimak
 */
public class ProcessScenarioTestReportGenerator extends AbstractProcessReport<ProcessScenarioTestReportGenerator> {

  private static final String SCENARIO_REPORT_PATH = "scenario.report.path";

  private final String path;
  private final String feature;
  private final String scenario;

  public ProcessScenarioTestReportGenerator(String feature, String scenario) {
    this(Paths.get(getPath()).toString(), feature, scenario);
  }

  public ProcessScenarioTestReportGenerator(String path, String feature, String scenario) {
    this.path = path;
    this.feature = feature;
    this.scenario = scenario;
  }

  @Override
  public ProcessScenarioTestReportGenerator generate(String deploymentId) {

    List<ProcessDefinition> processDefinitions =
      processEngine.getRepositoryService().createProcessDefinitionQuery()
        .deploymentId(deploymentId).list();

    for (ProcessDefinition processDefinition : processDefinitions) {

      List<HistoricProcessInstance> processInstances =
        processEngine.getHistoryService().createHistoricProcessInstanceQuery()
          .processDefinitionId(processDefinition.getId()).list();

      final int total = processInstances.size();

      if (total > 0) {

        String processDefinitionKey = processInstances.get(0).getProcessDefinitionKey();

        for (int i = 0; i < total; i++) {
          Path path = Paths.get(this.path, this.feature, this.scenario,
            String.format("%s%s%s.bpmn", processDefinitionKey, String.format("_%s", scenario), (total == 1) ? "" : String.format("_%s", i + 1)));
          BpmnModelInstance scenarioReport = Report.processScenarioReport().generate(processInstances.get(i).getId());
          writeReport(path, scenarioReport);
        }

        Path path = Paths.get(this.path, String.format("%s.bpmn", processDefinitionKey));
        BpmnModelInstance coverageReport = Report.processCoverageReport().generate(processDefinitionKey);
        writeReport(path, coverageReport);

      }

    }

    return this;

  }

  private void writeReport(Path path, BpmnModelInstance report) {
    try {
      Files.createDirectories(path.getParent());
      Bpmn.writeModelToFile(path.toFile(), report);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  private static String getPath() {
    Properties properties = new Properties();
    properties.setProperty(SCENARIO_REPORT_PATH, "./target/camunda-reports");
    InputStream propertiesFile = ProcessScenarioTestReportGenerator.class
      .getResourceAsStream("/camunda-platform-scenario.properties");
    if (propertiesFile != null) {
       try {
         properties.load(propertiesFile);
       } catch (IOException exception) {
         throw new RuntimeException(exception);
       }
    }
    String systemProperty = System.getProperty(SCENARIO_REPORT_PATH);
    if (systemProperty != null)
      properties.setProperty(SCENARIO_REPORT_PATH, systemProperty);
    return properties.getProperty(SCENARIO_REPORT_PATH);
  }

}
