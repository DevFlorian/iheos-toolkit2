package gov.nist.toolkit.xdstools2.client.tabs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import gov.nist.toolkit.configDatatypes.client.TransactionType;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.sitemanagement.client.SiteSpec;
import gov.nist.toolkit.xdstools2.client.CoupledTransactions;
import gov.nist.toolkit.xdstools2.client.command.command.FolderValidationCommand;
import gov.nist.toolkit.xdstools2.client.widgets.PopupMessage;
import gov.nist.toolkit.xdstools2.client.siteActorManagers.GetDocumentsSiteActorManager;
import gov.nist.toolkit.xdstools2.client.tabs.genericQueryTab.GenericQueryTab;
import gov.nist.toolkit.xdstools2.shared.command.request.FoldersRequest;

import java.util.ArrayList;
import java.util.List;

public class FolderTab extends GenericQueryTab {
	
	static List<TransactionType> transactionTypes = new ArrayList<TransactionType>();
	static {
		transactionTypes.add(TransactionType.STORED_QUERY);
		transactionTypes.add(TransactionType.IG_QUERY);
		transactionTypes.add(TransactionType.XC_QUERY);
	}
	
	static CoupledTransactions couplings = new CoupledTransactions();
	static {
		couplings.add(TransactionType.IG_QUERY, TransactionType.XC_QUERY, new HTML("Choose a Responding Gateway also, so that its homeCommunityId can be included in the request."),  "This request will be sent to %s and will include the homeCommunityId from %s.");
	}

	String help = "";

	public FolderTab() {
		super(new GetDocumentsSiteActorManager());
	}

	@Override
	protected Widget buildUI() {
		FlowPanel container=new FlowPanel();
		HTML title = new HTML();
		title.setHTML("<h2>Folder validation</h2>");
		container.add(title);

		mainGrid = new FlexTable();
		int row = 0;

		container.add(mainGrid);
		return container;
	}

	@Override
	protected void bindUI() {
	}

	@Override
	protected void configureTabView() {
		queryBoilerplate = addQueryBoilerplate(new Runner(), transactionTypes, couplings, true);
	}

	class Runner implements ClickHandler {

		public void onClick(ClickEvent event) {
			resultPanel.clear();

            SiteSpec siteSpec = queryBoilerplate.getSiteSelection();
            if (siteSpec == null) {
                new PopupMessage("You must select a site first");
                return;
            }

            if (pidTextBox.getValue() == null || pidTextBox.getValue().equals("")) {
                new PopupMessage("You must enter a Patient ID first");
                return;
            }
            addStatusBox();
            getGoButton().setEnabled(false);
            getInspectButton().setEnabled(false);

			new FolderValidationCommand(){
				@Override
				public void onComplete(List<Result> result) {
					queryCallback.onSuccess(result);
				}
			}.run(new FoldersRequest(getCommandContext(),siteSpec,pidTextBox.getValue().trim()));
		}
		
	}

	public String getWindowShortName() {
		return "folder";
	}



}
