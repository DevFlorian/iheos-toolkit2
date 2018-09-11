package gov.nist.toolkit.xdstools2.client.tabs.conformanceTest;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import gov.nist.toolkit.installation.shared.TestSession;
import gov.nist.toolkit.services.client.PifType;
import gov.nist.toolkit.services.client.RawResponse;
import gov.nist.toolkit.services.client.RegOrchestrationRequest;
import gov.nist.toolkit.services.client.RegOrchestrationResponse;
import gov.nist.toolkit.sitemanagement.client.SiteSpec;
import gov.nist.toolkit.xdstools2.client.command.command.BuildRegTestOrchestrationCommand;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.client.widgets.OrchestrationSupportTestsDisplay;
import gov.nist.toolkit.xdstools2.client.widgets.buttons.AbstractOrchestrationButton;
import gov.nist.toolkit.xdstools2.shared.command.request.BuildRegTestOrchestrationRequest;

import java.util.ArrayList;
import java.util.List;


/**
 * Build Registry tests orchestration
 */
public class BuildRegTestOrchestrationButton extends AbstractOrchestrationButton {
    private ConformanceTestTab testTab;
    private Panel initializationPanel;
    private TestContext testContext;
    private TestContextView testContextView;
    private FlowPanel initializationResultsPanel = new FlowPanel();
    private RadioButton noFeed = new RadioButton("pidFeedGroup", "No Patient Identity Feed");
    private RadioButton v2Feed = new RadioButton("pidFeedGroup", "V2 Patient Identitfy Feed");

    static private final String XADPID_OPTION = "xadpid"; // corresponds to collection reg_xadpid in testkit collections.txt file
    static private final String RM_OPTION = "rm"; // corresponds to collection reg_rm in testkit collections.txt file
    static private final String MU_OPTION = "mu";  // corresponds to collection reg_mu in testkit collections.txt file
    static private final String MPQ_OPTION = "mpq";
    static private final String OD_OPTION = "od";
    static private final String ISR_OPTION = "isr";
    static private final String CAT_FOLDER_OPTION = "catfolder";   // run as part of Connectathon
    static private final String CAT_LIFECYCLE_OPTION = "catlifecycle";   // run as part of Connectathon
    public static List<ActorAndOption> ACTOR_OPTIONS = new ArrayList<>();
    static {
        ACTOR_OPTIONS = java.util.Arrays.asList(
                new ActorAndOption("reg", "", "Required", false),
                new ActorAndOption("reg", MU_OPTION, "Metadata Update Option", false),
                new ActorAndOption("reg", MPQ_OPTION, "MPQ Option", false),
                new ActorAndOption("reg", OD_OPTION, "On Demand Option", false),
                new ActorAndOption("reg", RM_OPTION, "Remote Registry Option", false),
                new ActorAndOption("reg", ISR_OPTION, "Integrated Source Repository", true),
                new ActorAndOption("reg", XUA_OPTION, "XUA Option", false),
                new ActorAndOption("reg", CAT_FOLDER_OPTION, "CAT Folder", false),
                new ActorAndOption("reg", CAT_LIFECYCLE_OPTION, "CAT Lifecycle", false),
                new ActorAndOption("reg", XADPID_OPTION, "XAD-PID Required", false)
                
        );
    }

    BuildRegTestOrchestrationButton(ConformanceTestTab testTab, TestContext testContext, TestContextView testContextView, Panel initializationPanel, String label) {
        this.initializationPanel = initializationPanel;
        this.testTab = testTab;
        this.testContext = testContext;
        this.testContextView = testContextView;

        //
        // Disable selections that are not yet supported
        //
//        noFeed.setEnabled(false);

        setParentPanel(initializationPanel);

        FlowPanel pidFeedPanel = new FlowPanel();
        pidFeedPanel.add(noFeed);
        pidFeedPanel.add(v2Feed);
        v2Feed.setChecked(true);

        setCustomPanel(pidFeedPanel);
        build();
        panel().add(initializationResultsPanel);
        
        /** TODO - KM turned off**/
        //setCustomPanel(pidFeedPanel);
        /** TODO - End KM turned off**/
        /** TODO - KM turned off**/
        //build();
        /** TODO - End KM turned off**/
        /** TODO - KM turned off**/
        //panel().add(initializationResultsPanel);
        /** TODO - End KM turned off**/

    }

    public void orchestrate() {
        String msg = testContext.verifyTestContext();
        if (msg != null) {
            testContextView.launchDialog(msg);
            return;
        }

        initializationResultsPanel.clear();
        testTab.getMainView().showLoadingMessage("Initializing...");

        TestSession testSession = new TestSession(testTab.getCurrentTestSession());
        RegOrchestrationRequest request = new RegOrchestrationRequest();
        request.selfTest(isSelfTest());
        request.setPifType((v2Feed.isChecked()) ? PifType.V2 : PifType.NONE);
        request.setUseTls(isTls());
        request.setTestSession(testSession);
        request.setEnvironmentName(testTab.getEnvironmentSelection());
        request.setUseExistingState(!isResetRequested());
        SiteSpec sutSiteSpec = (testContext.getSiteUnderTest() == null) ? null : testContext.getSiteUnderTest().siteSpec();
        if (isSaml()) {
            setSamlAssertion(sutSiteSpec);
        }
        request.setRegistrySut(sutSiteSpec);

        testTab.setSiteToIssueTestAgainst(sutSiteSpec);

        new BuildRegTestOrchestrationCommand(){
            @Override
            public void onComplete(RawResponse rawResponse) {
                if (handleError(rawResponse, RegOrchestrationResponse.class)) {
                    testTab.getMainView().clearLoadingMessage();
                    return;
                }
                final RegOrchestrationResponse orchResponse = (RegOrchestrationResponse) rawResponse;
                testTab.setRegOrchestrationResponse(orchResponse);

                initializationResultsPanel.add(new HTML("Initialization Complete"));

                if (testContext.getSiteUnderTest() != null) {
                    initializationResultsPanel.add(new SiteDisplay("System Under Test Configuration", testContext.getSiteUnderTest()));
                }

                initializationResultsPanel.add(new HTML("<h2>Supporting Environment Configuration</h2>"));

                handleMessages(initializationResultsPanel, orchResponse);

                // Display tests run as part of orchestration - so links to their logs are available
                initializationResultsPanel.add(new OrchestrationSupportTestsDisplay(orchResponse, testContext, testContextView, testTab, testTab ));

                initializationResultsPanel.add(new HTML("<br />"));

                initializationResultsPanel.add(new HTML("Patient ID for Register tests: " + orchResponse.getRegisterPid().toString()));
                initializationResultsPanel.add(new HTML("Alternate Patient ID for Register tests: " + orchResponse.getRegisterAltPid().toString()));
                initializationResultsPanel.add(new HTML("Patient ID for Stored Query tests: " + orchResponse.getSqPid().toString()));
                initializationResultsPanel.add(new HTML("Patient ID for MPQ tests: " + orchResponse.getMpq1Pid().toString()));
                initializationResultsPanel.add(new HTML("Patient ID for MPQ tests: " + orchResponse.getMpq2Pid().toString()));
                initializationResultsPanel.add(new HTML("<br />"));

                testTab.displayTestCollection(testTab.getMainView().getTestsPanel());

            }
        }.run(new BuildRegTestOrchestrationRequest(ClientUtils.INSTANCE.getCommandContext(),request));
    }


}
