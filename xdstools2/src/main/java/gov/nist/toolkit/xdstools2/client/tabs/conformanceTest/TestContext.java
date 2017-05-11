package gov.nist.toolkit.xdstools2.client.tabs.conformanceTest;

import gov.nist.toolkit.sitemanagement.client.Site;
import gov.nist.toolkit.sitemanagement.client.SiteSpec;
import gov.nist.toolkit.xdstools2.client.ToolWindow;

/**
 *
 */
public class TestContext implements SiteManager {
    private ToolWindow toolWindow;
    private SiteSpec currentSiteSpec = new SiteSpec();
    private Site siteUnderTest = null;
    private TestContextView testContextView;
    static final protected String NONE = "--none--";
    private SiteSelectionValidator siteSelectionValidator = null;

    public TestContext(ToolWindow toolWindow, SiteSelectionValidator siteValidator) {
        this.toolWindow = toolWindow;
        this.siteSelectionValidator = siteValidator;
    }

    public void setTestContextView(TestContextView testContextView) {
        this.testContextView = testContextView;
    }

    public String verifyTestContext() {
        String msg;
        msg = verifyEnvironmentSelection();
        if (msg != null) return msg;

        msg = verifyTestSession();
        if (msg != null) return msg;

        msg = verifySite();
        if (msg != null) return msg;

        return null;  // good
    }

    private String verifyEnvironmentSelection() {
        if (toolWindow.getEnvironmentSelection() != null) return null;
        return "Environment must be selected before you proceed.";
    }

    private String verifyTestSession() {
        if (toolWindow.getCurrentTestSession() != null) return null;
        return "Test Session must be selected before you proceed.";
    }

    private String verifySite() {
        if (getSiteName() != null) {
            if (currentSiteSpec != null && siteSelectionValidator != null) {
                siteSelectionValidator.validate(currentSiteSpec);
            }
            return null;
        }
        return "System under test must be selected before you proceed.";
    }

    @Override
    public String getSiteName() {
        return currentSiteSpec.getName();
    }

    @Override
    public void setSiteName(String site) {
        setCurrentSiteSpec(site);
        /*
        if (site == null) return;
        if (site.equals(NONE)) return;
        new GetSiteCommand(){
            @Override
            public void onFailure(Throwable throwable) {
                new PopupMessage("System " + currentSiteSpec.getName() + " does not exist.");
                currentSiteSpec.setName(null);
                siteUnderTest = null;
                testContextView.updateTestingContextDisplay();
            }
            @Override
            public void onComplete(Site result) {
                siteUnderTest = result;
            }
        }.run(new GetSiteRequest(ClientUtils.INSTANCE.getCommandContext(),currentSiteSpec.getName()));
        */
    }

    @Override
    public void update() {
        testContextView.updateTestingContextDisplay();
    }


    public SiteSpec getCurrentSiteSpec() {
        return currentSiteSpec;
    }

    @Override
    public Site getSiteUnderTest() {
        return siteUnderTest;
    }

    public SiteSpec getSiteUnderTestAsSiteSpec() {
        return siteUnderTest.siteSpec();
    }

    public void setSiteUnderTest(Site siteUnderTest) {
        this.siteUnderTest = siteUnderTest;
    }

    public String getTestSession() {
        return toolWindow.getCurrentTestSession();
    }

    public void setSiteSelectionValidator(SiteSelectionValidator siteSelectionValidator) {
        this.siteSelectionValidator = siteSelectionValidator;
    }

    public void setCurrentSiteSpec(String siteName) {
        this.currentSiteSpec.setName(siteName);
    }
}
