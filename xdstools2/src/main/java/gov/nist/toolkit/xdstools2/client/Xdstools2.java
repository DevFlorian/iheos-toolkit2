package gov.nist.toolkit.xdstools2.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import gov.nist.toolkit.sitemanagement.client.TransactionOfferings;
import gov.nist.toolkit.tk.client.TkProps;
import gov.nist.toolkit.xdstools2.client.command.command.GetToolkitPropertiesCommand;
import gov.nist.toolkit.xdstools2.client.command.command.GetTransactionOfferingsCommand;
import gov.nist.toolkit.xdstools2.client.command.command.InitializationCommand;
import gov.nist.toolkit.xdstools2.client.event.testSession.TestSessionManager2;
import gov.nist.toolkit.xdstools2.client.injector.Injector;
import gov.nist.toolkit.xdstools2.client.selectors.CasUserTestSessionSelector;
import gov.nist.toolkit.xdstools2.client.selectors.EnvironmentManager;
import gov.nist.toolkit.xdstools2.client.selectors.MultiUserTestSessionSelector;
import gov.nist.toolkit.xdstools2.client.selectors.TestSessionSelector;
import gov.nist.toolkit.xdstools2.client.tabs.EnvironmentState;
import gov.nist.toolkit.xdstools2.client.tabs.HomeTab;
import gov.nist.toolkit.xdstools2.client.tabs.QueryState;
import gov.nist.toolkit.xdstools2.client.tabs.messageValidator.MessageValidatorTab;
import gov.nist.toolkit.xdstools2.client.util.ClientFactory;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.client.util.TabWatcher;
import gov.nist.toolkit.xdstools2.client.widgets.PopupMessage;
import gov.nist.toolkit.xdstools2.shared.command.InitializationResponse;

import javax.inject.Inject;
import java.util.Map;
import java.util.logging.Logger;


public class Xdstools2  implements AcceptsOneWidget, IsWidget, RequiresResize, ProvidesResize, TabWatcher {
	public static final int TRAY_SIZE = 190;
	public static final int TRAY_CTL_BTN_SIZE = 9; // 23

	private static Xdstools2 ME = new Xdstools2();

	private static final ClientFactory clientFactory = GWT.create(ClientFactory.class);
	private final static Logger logger=Logger.getLogger(Xdstools2.class.getName());

	public SplitLayoutPanel mainSplitPanel = new SplitLayoutPanel(3);
	private FlowPanel mainMenuPanel = new FlowPanel();
	static final HomeTab ht = new HomeTab();

	private HorizontalPanel uiDebugPanel = new HorizontalPanel();
	boolean UIDebug = false;
	private boolean displayHomeTab = true;

	public static String toolkitBaseUrl = null;
	public static String wikiBaseUrl = null;

	private static TkProps props = new TkProps();

	public Xdstools2() {
	}

	static public Xdstools2 getInstance() {
		if (ME==null){
			ME=new Xdstools2();
		}
		return ME;
	}

	public void doNotDisplayHomeTab() { displayHomeTab = false; }

	// This bus is used for v2 v3 integration that signals v2 launch tab event inside the v3 environment
	EventBus v2V3IntegrationEventBus = null;

	// This is as toolkit wide singleton.  See class for details.
	public TestSessionManager2 getTestSessionManager() {
		return ClientUtils.INSTANCE.getTestSessionManager();
	}

	// Central storage for parameters shared across all
	// query type tabs
	QueryState queryState = new QueryState();
	public QueryState getQueryState() {
		return queryState;
	}

	static public TransactionOfferings transactionOfferings = null;

	@Inject
	private TabContainer tabContainer;

	void buildTabsWrapper() {
		final HorizontalPanel menuPanel = new HorizontalPanel();
		tabContainer = Injector.INSTANCE.getTabContainer();
		assert(tabContainer != null);
		final EnvironmentManager environmentManager = new EnvironmentManager(tabContainer);

		Widget decoratedTray = makeMenuCollapsible();

		mainSplitPanel.addWest(decoratedTray, TRAY_SIZE);
		mainSplitPanel.setWidgetToggleDisplayAllowed(decoratedTray,true);


		TabContainer.setWidth("100%");
		TabContainer.setHeight("100%");

		new GetToolkitPropertiesCommand() {
			@Override
			public void onFailure(Throwable throwable) {
				new PopupMessage("BuildTabsWrapper error getting properties : " + throwable.toString());
			}

			@Override
			public void onComplete(final Map<String, String> tkPropMap) {
				ClientUtils.INSTANCE.setTkPropMap(tkPropMap);
				boolean multiUserModeEnabled = Boolean.parseBoolean(tkPropMap.get("Multiuser_mode"));
				boolean casModeEnabled = Boolean.parseBoolean(tkPropMap.get("Cas_mode"));

				// No environment selector for CAS mode, but allow for single user mode and multi user mode
				if (!casModeEnabled) {
					menuPanel.add(environmentManager);
					menuPanel.setSpacing(10);
				}

				// Only single user mode has a selectable test session drop down
				if (!multiUserModeEnabled && !casModeEnabled) {
					getTestSessionManager().setCurrentTestSession("default");
					menuPanel.add(new TestSessionSelector(getTestSessionManager().getTestSessions(), getTestSessionManager().getCurrentTestSession()).asWidget());
				} else {
					if (multiUserModeEnabled && !casModeEnabled) {
						// Only two options: 1) Change Test Session and 2) New Test Session
						menuPanel.add(new MultiUserTestSessionSelector( Xdstools2.this).asWidget());
					} else if (casModeEnabled) {
						// Only one option: 1) Change Test Session
						menuPanel.add(new CasUserTestSessionSelector(Xdstools2.this).asWidget());
					}
				}

			}
		}.run(ClientUtils.INSTANCE.getCommandContext());



		DockLayoutPanel mainPanel = new DockLayoutPanel(Style.Unit.EM);
		mainPanel.addNorth(menuPanel, 4);
		mainPanel.add(TabContainer.getTabPanel());
		mainSplitPanel.add(mainPanel);

		Window.addResizeHandler(new ResizeHandler() {
			@Override
			public void onResize(ResizeEvent event) {
				resizeToolkit();
			}

		});
	}

	static public void addtoMainMenu(Widget w) { ME.mainMenuPanel.add(w); }

	static public void clearMainMenu() { ME.mainMenuPanel.clear(); }

	private Widget makeMenuCollapsible() {
		final FlowPanel vpCollapsible =  new FlowPanel();

		// Set margins
		mainMenuPanel.getElement().getStyle().setMargin(3, Style.Unit.PX);

		// Make it Collapsible

		final HTML menuTrayStateToBe = new HTML("&laquo;");
		menuTrayStateToBe.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT); // Right alignment causes a problem where it is cut off (non-intuitively hidden over current view but still accessible by the horizontal scrollbar) by longer menu item labels that wrap in a smaller-sized browser window
		menuTrayStateToBe.getElement().getStyle().setFontSize(14, Style.Unit.PX);
		menuTrayStateToBe.setTitle("Minimize Toolkit Menu");

		// Make it rounded
		menuTrayStateToBe.setStyleName("menuCollapse");
		menuTrayStateToBe.setWidth("15px");
		menuTrayStateToBe.setWidth("100%");

		vpCollapsible.add(menuTrayStateToBe);
		vpCollapsible.add(mainMenuPanel);

		// Wrap menu in a scroll panel
		final ScrollPanel spMenu = new ScrollPanel(vpCollapsible);

		menuTrayStateToBe.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (menuTrayStateToBe.getHTML().hashCode()==171) {
					menuTrayStateToBe.setHTML("&raquo;");
					menuTrayStateToBe.setTitle("Expand Toolkit Menu");
					mainMenuPanel.setVisible(false);
					menuTrayStateToBe.setWidth("");
					menuTrayStateToBe.setHeight(mainSplitPanel.getElement().getClientHeight()+"px");
					mainSplitPanel.setWidgetSize(spMenu, TRAY_CTL_BTN_SIZE);
				} else {
					menuTrayStateToBe.setHTML("&laquo;");
					menuTrayStateToBe.setTitle("Collapse");
					mainMenuPanel.setVisible(true);
					menuTrayStateToBe.setWidth("100%");
					menuTrayStateToBe.setHeight("");
					mainSplitPanel.setWidgetSize(spMenu,TRAY_SIZE);
				}
			}
		});

		return spMenu;
	}

	public void resizeToolkit() {
	}

	static boolean newHomeTab = false;

	@Deprecated
	static public TkProps tkProps() {
		return props;
	}

	/**
	 * This is the old entry point method.
	 * It's now being used as an initialization method the GUI and the environment
	 */
	void run() {
		new InitializationCommand() {

			@Override
			public void onComplete(InitializationResponse var1) {
				// default environment
				// environment names
				// test session names
				toolkitName = var1.getServletContextName();
				EnvironmentState environmentState= ClientUtils.INSTANCE.getEnvironmentState();
				environmentState.setEnvironmentNameChoices(var1.getEnvironments());
				if (environmentState.getEnvironmentName() == null)
					environmentState.setEnvironmentName(var1.getDefaultEnvironment());
				getTestSessionManager().setTestSessions(var1.getTestSessions());
				toolkitBaseUrl = var1.getToolkitBaseUrl();
				wikiBaseUrl = var1.getWikiBaseUrl();
				run2();  // cannot be run until this completes
			}

			// this is included because even if init fails (bad EC location for example)
			// startup must continue
			@Override
			public void onFailure(Throwable throwable) {
				String msg = throwable.getMessage();
				if (msg == null)
					msg = this.getClass().getName();
				new PopupMessage("Request to server failed: " + msg);

				run2();  // cannot be run until this completes
			}
		}.run(getHomeTab().getCommandContext());  // command context will be ignored by this cmd
	}

	private void run2() {
		buildTabsWrapper();

		// If using ConfActor activity then home tab is a distraction
		if (!displayHomeTab)
			ht.setDisplayTab(false);
		ht.onTabLoad(false, "Home");

		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			public void onValueChange(ValueChangeEvent<String> event) {
				String historyToken = event.getValue();

				// Parse the history token
				try {
					if (historyToken.equals("mv")) {
						new MessageValidatorTab().onTabLoad(true, "MsgVal");
					}

				} catch (IndexOutOfBoundsException e) {
					TabContainer.selectTab(0);
				}
			}
		});


		String currentTestSession = getTestSessionManager().fromCookie();
		if (currentTestSession == null)
			currentTestSession = "default";
		if (getTestSessionManager().isLegalTestSession(currentTestSession)) {
			// Don't overwrite initialization by ConfActor activity
			if (getTestSessionManager().getCurrentTestSession() == null)
				getTestSessionManager().setCurrentTestSession(currentTestSession);
		}
//		testSessionManager.load();
//		loadServletContext();
		reloadTransactionOfferings();
	}

	public String toolkitName;

//	private void loadServletContext() {
//		ht.toolkitService.getServletContextName(new AsyncCallback<String>() {
//			@Override
//			public void onFailure(Throwable throwable) {
//				new PopupMessage("Failed to load servletContextName - " + throwable.getMessage());
//			}
//
//			@Override
//			public void onSuccess(String s) {
//				toolkitName = s;
//			}
//		});
//	}

	/**
	 * request must be attached to a tab so use home tab
	 * since it's available
	 */
	private void reloadTransactionOfferings() {
		new GetTransactionOfferingsCommand() {

			@Override
			public void onComplete(TransactionOfferings var1) {
				transactionOfferings = var1;
			}
		}.run(getHomeTab().getCommandContext());
	}

//	static public EventBus getEventBus() {
//		return clientFactory.getEventBus();
//	}

	// To force an error when I merge with Sunil. We need
	// to reconcile the initialization.
	public void setIntegrationEventBus(EventBus eventBus) {
		v2V3IntegrationEventBus = eventBus;
	}
	public EventBus getIntegrationEventBus() {
		return v2V3IntegrationEventBus;
	}

	@Override
	public void setWidget(IsWidget isWidget) {
		mainSplitPanel.add(isWidget.asWidget());
	}

	@Override
	public Widget asWidget() {
		return mainSplitPanel;
	}

	public static HomeTab getHomeTab() { return ht; }

	@Override
	public void onResize() {
	 mainSplitPanel.onResize();
	}

	@Override
	public int getTabCount() {
	   return tabContainer.getTabCount();
	}

	@Override
	public boolean closeAllTabs() {
	    return tabContainer.closeAllTabs();
	}
}
