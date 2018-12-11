package gov.nist.toolkit.xdstools2.client.tabs.conformanceTest;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import gov.nist.toolkit.actortransaction.client.ParamType;
import gov.nist.toolkit.actortransaction.shared.ActorType;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.configDatatypes.server.SimulatorProperties;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.services.client.EsOrchestrationRequest;
import gov.nist.toolkit.services.client.EsOrchestrationResponse;
import gov.nist.toolkit.services.client.RawResponse;
import gov.nist.toolkit.simcommon.client.SimulatorConfig;
import gov.nist.toolkit.simcommon.client.config.SimulatorConfigElement;
import gov.nist.toolkit.xdstools2.client.command.command.BuildEsTestOrchestrationCommand;
import gov.nist.toolkit.sitemanagement.client.SiteSpec;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.client.widgets.buttons.AbstractOrchestrationButton;
import gov.nist.toolkit.xdstools2.shared.command.request.BuildEsTestOrchestrationRequest;

public class BuildEsTestOrchestrationButton extends AbstractOrchestrationButton {
    private ConformanceTestTab testTab;
    private Panel initializationPanel;
    private FlowPanel initializationResultsPanel = new FlowPanel();
    private TestContext testContext;
    private TestContextView testContextView;

    BuildEsTestOrchestrationButton(ConformanceTestTab testTab, TestContext testContext, TestContextView testContextView, Panel initializationPanel, String label) {
        this.initializationPanel = initializationPanel;
        this.testTab = testTab;
        this.testContext = testContext;
        this.testContextView = testContextView;


        HTML instructions = new HTML(
                "<p>" +
                "The System Under Test (SUT) is an Edge Server..." +
                "</p>");

        initializationPanel.add(instructions);

        setSystemDiagramUrl("diagrams/IIGdiagram.png");

        setParentPanel(initializationPanel);
        setLabel(label);
        setResetLabel("Reset");
        build();
        panel().add(initializationResultsPanel);
    }
    public void orchestrate() {
        String msg = testContext.verifyTestContext();
        if (msg != null) {
            testContextView.launchDialog(msg);
            return;
        }

        initializationResultsPanel.clear();
        testTab.getMainView().showLoadingMessage("Initializing...");

        EsOrchestrationRequest request = new EsOrchestrationRequest();
        request.setTestSession(new TestSession(testTab.getCurrentTestSession()));
        request.setEnvironmentName(testTab.getEnvironmentSelection());
        request.setUseExistingState(!isResetRequested());
        SiteSpec siteSpec = new SiteSpec(testContext.getSiteName(), new TestSession(testTab.getCurrentTestSession()));
        if (isSaml()) {
            setSamlAssertion(siteSpec);
        }
        request.setSiteUnderTest(siteSpec);

        testTab.setSiteToIssueTestAgainst(siteSpec);

        new BuildEsTestOrchestrationCommand(){
            @Override
            public void onComplete(RawResponse rawResponse) {
                if (handleError(rawResponse, EsOrchestrationResponse.class)) {
                    testTab.getMainView().clearLoadingMessage();
                    return;
                }
                EsOrchestrationResponse orchResponse = (EsOrchestrationResponse) rawResponse;
                testTab.setOrchestrationResponse(orchResponse);

                initializationResultsPanel.add(new HTML("Initialization complete"));

                if (testContext.getSiteUnderTest() != null) {
                    initializationResultsPanel.add(new HTML("<h2>System Under Test Configuration</h2>"));
                    initializationResultsPanel.add(new HTML("Site: " + testContext.getSiteUnderTest().getName()));
                    FlexTable table = new FlexTable();
                    int row = 0;
                    table.setText(row, 0, "Retrieve Img Doc Set: ");
                    try {
                        table.setText(row++ , 1,
                                testContext.getSiteUnderTest().getRawEndpoint(TransactionType.RET_IMG_DOC_SET_GW, false, false));
                    } catch (Exception e) {}

                    initializationResultsPanel.add(table);
                }

                initializationResultsPanel.add(new HTML("<h2>Generated Environment</h2>"));

                FlexTable table = new FlexTable();
                int row = 0;
                // Pass through simulators in Orchestra enum order
                for (Orchestra o : Orchestra.values()) {
                    // get matching simulator config
                    SimulatorConfig sim = null;
                    for (SimulatorConfig c : orchResponse.getSimulatorConfigs()) {
                        if (c.getId().getId().equals(o.name())) {
                            sim = c;
                            break;
                        }
                    }
                    if (sim == null) continue;

                    try {
                        // First row: title, sim id, test data and log buttons
                        table.setWidget(row, 0, new HTML("<h3>" + o.title + "</h3>"));
                        table.setText(row++ , 1, sim.getId().toString());

                        // Property rows, based on ActorType and Orchestration enum
                        for (String property : o.getDisplayProps()) {
                            table.setWidget(row, 1, new HTML(property));
                            SimulatorConfigElement prop = sim.get(property);
                            String value = prop.asString();
                            if (prop.hasList()) value = prop.asList().toString();
                            table.setWidget(row++ , 2, new HTML(value));
                        }
                    } catch (Exception e) {
                        initializationResultsPanel.add(new HTML("<h3>exception " + o.name() + " " + e.getMessage() + "/h3>"));
                    }
                }
                initializationResultsPanel.add(table);

                initializationResultsPanel.add(new HTML("<p>Configure your " +
                        "Edge Server with these endpoints<hr/>"));

                testTab.displayTestCollection(testTab.getMainView().getTestsPanel());
            }
        }.run(new BuildEsTestOrchestrationRequest(ClientUtils.INSTANCE.getCommandContext(),request));
    }


    public enum Orchestra {

        rr("Repository Registry", ActorType.REPOSITORY_REGISTRY,
                new SimulatorConfigElement[] {
                        new SimulatorConfigElement(SimulatorProperties.VALIDATE_AGAINST_PATIENT_IDENTITY_FEED, ParamType.BOOLEAN,
                                false),
                        new SimulatorConfigElement(SimulatorProperties.repositoryUniqueId, ParamType.TEXT,
                                "1.3.6.1.4.1.21367.13.71.101.1") }),

        ids("Imaging Document Source", ActorType.IMAGING_DOC_SOURCE,
                new SimulatorConfigElement[] {
                        new SimulatorConfigElement(SimulatorProperties.idsRepositoryUniqueId, ParamType.TEXT,
                                "1.3.6.1.4.1.21367.102.1.1"),
                        new SimulatorConfigElement(SimulatorProperties.idsImageCache, ParamType.TEXT, "ids-dataset-a") });

        public final String title;
        public final ActorType actorType;
        public final SimulatorConfigElement[] elements;

        Orchestra(String title, ActorType actorType, SimulatorConfigElement[] elements) {
            this.title = title;
            this.actorType = actorType;
            this.elements = elements;
        }

        public ActorType getActorType() {
            return actorType;
        }

        public SimulatorConfigElement[] getElements() {
            return elements;
        }

        /**
         * @return array of Simulator Property names which should be displayed in
         * Conformance testing for this type of actor.
         */
        public String[] getDisplayProps() {
            switch (actorType) {
                case IMAGING_DOC_SOURCE:
                    return new String[] {
                            SimulatorProperties.idsRepositoryUniqueId,
                            SimulatorProperties.idsrEndpoint,
                            SimulatorProperties.wadoEndpoint,
                            SimulatorProperties.idsImageCache, };
                case REPOSITORY_REGISTRY:
                    return new String[] {
                            SimulatorProperties.retrieveEndpoint,
                            SimulatorProperties.storedQueryEndpoint,
                            SimulatorProperties.repositoryUniqueId, };
                default:
            }
            return new String[0];
        }

    } // EO Orchestra enum

}
