package gov.nist.toolkit.xdstools2.client.tabs.GatewayTestsTabs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.actortransaction.client.ActorType;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.installation.shared.TestCollectionCode;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.results.client.TestInstance;
import gov.nist.toolkit.services.client.RgOrchestrationResponse;
import gov.nist.toolkit.sitemanagement.client.SiteSpec;
import gov.nist.toolkit.sitemanagement.client.TransactionOfferings;
import gov.nist.toolkit.xdstools2.client.CoupledTransactions;
import gov.nist.toolkit.xdstools2.client.TabContainer;
import gov.nist.toolkit.xdstools2.client.command.command.GetTestResultsCommand;
import gov.nist.toolkit.xdstools2.client.command.command.RunMesaTestCommand;
import gov.nist.toolkit.xdstools2.client.inspector.MetadataInspectorTab;
import gov.nist.toolkit.xdstools2.client.siteActorManagers.GetDocumentsSiteActorManager;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;
import gov.nist.toolkit.xdstools2.client.widgets.PopupMessage;
import gov.nist.toolkit.xdstools2.shared.command.request.GetTestResultsRequest;
import gov.nist.toolkit.xdstools2.shared.command.request.RunTestRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RGTestTab extends GenericQueryTab implements GatewayTool {
//    final protected ToolkitServiceAsync toolkitService = GWT
//            .create(ToolkitService.class);

    private static CoupledTransactions couplings = new CoupledTransactions();

    //    TextBox patientIdBox = new TextBox();
    private String selectedActor = ActorType.RESPONDING_GATEWAY.getShortName();
    private GenericQueryTab genericQueryTab;
    private static final TestCollectionCode COLLECTION_NAME =  new TestCollectionCode("rgtool");
    private final TestSelectionManager testSelectionManager;
    private Panel siteSelectionPanel = new VerticalPanel();

    private String systemTypeGroup = "System Type Group";
    private RadioButton exposed = new RadioButton(systemTypeGroup, "Exposed Registry/Repository");
    private RadioButton external = new RadioButton(systemTypeGroup, "External Registry/Repository");
    boolean isExposed() { return exposed.getValue(); }
    boolean isExternal() { return external.getValue(); }
    boolean usingExposedRR() { return exposed.getValue(); }
    RgOrchestrationResponse orch;

    public RGTestTab() {
        super(new GetDocumentsSiteActorManager());
        testSelectionManager = new TestSelectionManager(this);
    }

//    @Override
//    public ToolkitServiceAsync getToolkitService() { return toolkitService; }

    @Override
    public TabContainer getToolContainer() { return getTabContainer(); }

    public void onTabLoad(TabContainer container, boolean select) {
    }

    @Override
    protected Widget buildUI() {
        return null;
    }

    @Override
    protected void bindUI() {

    }

    @Override
    protected void configureTabView() {

    }

    @Override
    public void onTabLoad(boolean select, String eventName) {
        genericQueryTab = this;

        registerTab(select, eventName);
        tlsOptionEnabled = false;

        // customization of GenericQueryTab
        autoAddRunnerButtons = false;  // want them in a different place
        genericQueryTitle = "Select System Under Test";
        addResultsPanel = false;  // manually done below




        ////////////////////////////////////////////////////////////////////////////////////////////////
        tabTopPanel.add(new HTML("<h1>Responding Gateway Test Tool</h1>"));

        tabTopPanel.add(new HTML(
                "This tool tests a Responding Gateway that exposes endpoints for a Document Registry and " +
                        "Document Repository or can be configured to use an external Registry/Repository pair. " +
                        "All tests are initiated by a Toolkit supplied Initiating Gateway that sends " +
                        "query and retrieve requests to the Responding Gateway/System Under Test." +


                "<h2>Create supporting test session</h2>" +
                        "These simulators and " +
                        "their logs will be maintained in a test session you create for this test. At the top of the window, " +
                        "create a new test session and select it. " +
                        "All context for this test is kept within this test session - if multiple test sessions are " +
                        "created they do not interact." +
                        "</p>"
        ));

        ////////////////////////////////////////////////////////////////////////////////////////////////
//        tabTopPanel.display(new HTML(
//                "<hr /><h2>System under test</h2>" +
//                "<p>" +
//                "The system under test is a Responding Gateway. To be testable by this tool one of the following " +
//                "configurations must be supported by your implementation. " +
//                "<ul>" +
//                "<li>Exposed Registry/Repository endpoints - your implementation includes Registry/Repository " +
//                "functionality and you expose the required endpoints for these actors." +
//                "A single site (system configuration in toolkit) must contain the Responding Gateway " +
//                "(system under test), and the related Registry and Repository configurations." +
//                "<li>External Registry/Repository - your implementation can be configured to work with an " +
//                "external Registry and Repository which will be selected below." +
//                "</ul>"
//        ));

        Panel systemTypePanel = new HorizontalPanel();
        systemTypePanel.add(exposed);
        systemTypePanel.add(external);
        Panel editExposedSystemConfigPanel = new HorizontalPanel();
        systemTypePanel.add(editExposedSystemConfigPanel);
        final Button editExposedSiteButton = new Button("Edit Site Configuration");
        editExposedSystemConfigPanel.add(editExposedSiteButton);
        editExposedSiteButton.setVisible(false);
        editExposedSiteButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {

            }
        });

        // exposed means using reg/rep from same site as RG
        exposed.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
//                editExposedSiteButton.setVisible(true);
            }
        });

        // external means using reg/rep from different site
        external.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> valueChangeEvent) {
//                editExposedSiteButton.setVisible(false);
            }
        });


        Panel instructionsPanel = new VerticalPanel();
        HTML instructions = new HTML(
                "<p>" +
                        "The system under test is a Responding Gateway. For the testing to be fully automated by this tool one of the following " +
                        "configurations must be supported by your implementation. " +
                        "<ul>" +
                        "<li>Exposed Registry/Repository endpoints - your implementation includes Registry/Repository " +
                        "functionality and you expose the required endpoints for these actors. " +
                        "A single site (system configuration in toolkit) must contain the Responding Gateway " +
                        "(system under test), and the related Registry and Repository configurations." +
                        "<li>External Registry/Repository - your implementation can be configured to work with an " +
                        "external Registry and Repository which will be selected below. This tool will provide " +
                        "these actors." +
                        "</ul>" +

                        "<p>If your Responding Gateway does not meet the above requirement it must be initialized " +
                        "manually.  See <a href=\"site/testkit/tests/RG/testdata.html\"  target=\"_blank\">here</a> for details.</p>"  +

                        "<p>When the test is run a Cross Gateway Query or Retrieve transaction will be sent to the " +
                        "Responding Gateway " +
                        "selected below. This will start the test. Before running a test, make sure your " +
                        "Responding Gateway is configured to forward requests to the Document Repository and Document Registry above.  This " +
                        "test only uses non-TLS endpoints (for now). TLS selection is disabled.</p>"
        );

        instructionsPanel.add(instructions);
        instructionsPanel.add(systemTypePanel);
        genericQueryInstructions = instructionsPanel;
        ////////////////////////////////////////////////////////////////////////////////////////////////
        // Query boilerplate
        ActorType act = ActorType.findActor(selectedActor);

        List<TransactionType> tt = act.getTransactions();

        // has to be before addQueryBoilerplate() which
        // references mainGrid
        mainGrid = new FlexTable();

        queryBoilerplate = addQueryBoilerplate(
                new Runner(),
                tt,
                new CoupledTransactions(),
                false  /* display patient id param */);


        ////////////////////////////////////////////////////////////////////////////////////////////////
        tabTopPanel.add(new HTML(
                        "<h2>Build Test Environment</h2>" +
                        "<p>" +
                        "This will initialize the test environment in the Test Session seleted at the top. " +
                        "The generated test environment will be displayed below. " +
                        "Once the test environment is built, configure your Responding Gateway to forward requests " +
                        "to the generated Registry/Repository simulators (if the External option is selected). " +
                        "</p>"
        ));

        HorizontalPanel testEnvironmentsPanel = new HorizontalPanel();
        tabTopPanel.add(testEnvironmentsPanel);

        BuildRGTestOrchestrationButton testEnvButton = new BuildRGTestOrchestrationButton(this, testEnvironmentsPanel, "Build Test Environment", false);

        tabTopPanel.add(new HTML("<hr />"));

        tabTopPanel.add(testSelectionManager.buildTestSelector());

        tabTopPanel.add(testSelectionManager.buildSectionSelector());

        tabTopPanel.add(mainGrid);

        testSelectionManager.loadTestsFromCollection(COLLECTION_NAME);


        ////////////////////////////////////////////////////////////////////////////////////////////////
        tabTopPanel.add(new HTML(
                "<hr />" +
                        "<h2>Run Test</h2>" +
                        "<p>" +
                        "Initiate the test from the Toolkit Document Consumer. After the test is run " +
                        "the Document Consumer's logs can be displayed with Inspect Results." +
                        "</p>"
        ));

        addRunnerButtons(tabTopPanel);

        tabTopPanel.add(resultPanel);
    }


    void buildExternalRegistryRepository() {

    }

    class SiteDisplayer implements CompletionHandler<TransactionOfferings> {

        @Override
        public void OnCompletion(TransactionOfferings transactionOfferings) {
            siteSelectionPanel.add(new HTML("<h2>Responding Gateway support actors</h2>" +
                    "The Responding Gateway (system under test) must be supported by a Document Repository and Document Registry " +
                    "that are publicly accessible so that test data can be loaded into them. Select the Repository and Registry " +
                    "supporting your Responding Gateway. If they are not on this selection list then update toolkit adding " +
                    "their configurations."
            ));



            siteSelectionPanel.add(new HTML("<h3>Repository Selection</h3>"));
            List<TransactionType> transactionTypes1 = new ArrayList<TransactionType>();
            transactionTypes1.add(TransactionType.PROVIDE_AND_REGISTER);
            siteSelectionPanel.add(new SiteSelectionWidget(transactionOfferings, transactionTypes1, couplings, getCurrentTestSession()).build(null, ""));


            siteSelectionPanel.add(new HTML("<h3>Registry Selection</h3>"));
            List<TransactionType> transactionTypes2 = new ArrayList<TransactionType>();
            transactionTypes2.add(TransactionType.REGISTER);
            siteSelectionPanel.add(new SiteSelectionWidget(transactionOfferings, transactionTypes2, couplings, getCurrentTestSession()).build(null, ""));
        }
    }


    class Runner implements ClickHandler {

        public void onClick(ClickEvent event) {

            resultPanel.clear();

            if (getCurrentTestSession().isEmpty()) {
                new PopupMessage("Test Session must be selected");
                return;
            }

            if (!verifySiteProvided()) return;

            addStatusBox();
            getGoButton().setEnabled(false);
            getInspectButton().setEnabled(false);

            List<String> selectedSections = testSelectionManager.getSelectedSections();

            Map<String, String> parms = new HashMap<>();
            parms.put("$testdata_home$", orch.getSiteUnderTest().homeId);

            Panel logLaunchButtonPanel = rigForRunning();
            logLaunchButtonPanel.clear();
            String testToRun = selectedTest;
            if (TestSelectionManager.ALL.equals(testToRun)) {
                testToRun = "tc:" + COLLECTION_NAME;
            }

            TestInstance testInstance = new TestInstance(testToRun);
            TestSession testSession = new TestSession(getCurrentTestSession());
            testInstance.setTestSession(testSession);
            new RunMesaTestCommand(){
                @Override
                public void onComplete(List<Result> result) {
                    queryCallback.onSuccess(result);
                }
            }.run(new RunTestRequest(getCommandContext(),getSiteSelection(),new TestInstance(testToRun),parms,true,selectedSections));
        }

    }

    Button addTestEnvironmentInspectorButton(final String siteName) {
        final TestSession testSession = new TestSession(getCurrentTestSession());
        Button button = new Button("Inspect Test Data - " + siteName);
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                List<TestInstance> tests = new ArrayList<TestInstance>();
                tests.add(new TestInstance("15807"));
                new GetTestResultsCommand(){
                    @Override
                    public void onComplete(Map<String, Result> resultMap) {
                        Result result = resultMap.get("15807");
                        if (result == null) {
                            new PopupMessage("Results not available");
                            return;
                        }
                        SiteSpec siteSpec = new SiteSpec(siteName, ActorType.RESPONDING_GATEWAY, null, testSession);

                        MetadataInspectorTab itab = new MetadataInspectorTab();
                        List<Result> results = new ArrayList<Result>();
                        results.add(result);
                        itab.setResults(results);
                        itab.setSiteSpec(siteSpec);
                        itab.onTabLoad(true, "Insp");
                    }
                }.run(new GetTestResultsRequest(getCommandContext(),tests));
            }
        });
        return button;
    }

    public String getWindowShortName() {
        return "igtests";
    }

}
