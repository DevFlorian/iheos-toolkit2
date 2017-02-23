package gov.nist.toolkit.xdstools2.client.widgets;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.*;
import gov.nist.toolkit.configDatatypes.client.Pid;
import gov.nist.toolkit.xdstools2.client.command.command.RetrieveFavPidsCommand;
import gov.nist.toolkit.xdstools2.client.event.FavoritePidsUpdatedEvent;
import gov.nist.toolkit.xdstools2.client.event.Xdstools2EventBus;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;
import gov.nist.toolkit.xdstools2.client.util.CookiesServices;
import gov.nist.toolkit.xdstools2.shared.command.CommandContext;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by onh2 on 7/11/16.
 */
public class PidFavoritesCellList extends Composite{
    private SimplePanel container = new SimplePanel();
    // List widget.
    private CellList<Pid> cellList;
    // list data model.
    private ListDataProvider<Pid> model = new ListDataProvider<>();
    // Selection model.
    private SingleSelectionModel<Pid> selectionModel = new SingleSelectionModel<>();

    // Key provider to id each Pid in the list.
    private static final ProvidesKey<Pid> KEY_PROVIDER = new ProvidesKey<Pid>() {
        @Override
        public String getKey(Pid pid) {
            return pid.toString();
        }
    };

    /**
     * Default constructor
     */
    public PidFavoritesCellList(){
        // Create a Cell renderer.
        PidCell pidCell = new PidCell();

        // Set a key provider that provides a unique key for each pid. If key is
        // used to identify pid.
        cellList = new CellList<>(pidCell,KEY_PROVIDER);

        // this links the data model with the actual table widget
        model.addDataDisplay(cellList);
        cellList.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.BOUND_TO_SELECTION);
//cellList.setSize("300px","400px");
        ScrollingPager pager = new ScrollingPager();
        pager.setSize("500px","400px");
        pager.setDisplay(cellList);
        // Add a selection model so we can select cells.
        cellList.setSelectionModel(selectionModel);

        container.add(pager);

        container.addStyleName("list-border");

        initWidget(container);

        bindUI();
    }

    private void bindUI() {
        // this refresh the list of PIDs after a new PID is added though the PID Manager.
        ((Xdstools2EventBus) ClientUtils.INSTANCE.getEventBus()).addFavoritePidsUpdateEventHandler(new FavoritePidsUpdatedEvent.FavoritePidsUpdatedEventHandler() {
            @Override
            public void onFavPidsUpdate() {
                model.setList(new LinkedList<Pid>(CookiesServices.retrievePidFavoritesFromCookies()));
                model.refresh();
                cellList.redraw();
            }
        });
        loadData();
    }

    /**
     * This method loads all the PIDs for the of favorites from both Cookies and the server.
     */
    private void loadData(){
        String environmentName = ClientUtils.INSTANCE.getEnvironmentState().getEnvironmentName();
        if (environmentName!=null) {
            new RetrieveFavPidsCommand() {
                @Override
                public void onComplete(List<Pid> pids) {
                    List<Pid> pidList=new LinkedList<>();
                    pidList.addAll(pids);
                    // load pids stored in cookies
                    pidList.addAll(CookiesServices.retrievePidFavoritesFromCookies());
                    model.setList(new LinkedList<>(pidList));
                    model.refresh();
                    cellList.redraw();
                }
            }.run(new CommandContext(environmentName, ClientUtils.INSTANCE.getTestSessionManager().getCurrentTestSession()));
        }
    }

    public void addSelectionChangeHandler(SelectionChangeEvent.Handler handler){
        selectionModel.addSelectionChangeHandler(handler);
    }

    public Pid getSelectedPid(){
        return selectionModel.getSelectedObject();
    }

    /**
     * This clear the list widget of all selection.
     */
    public void clearSelection() {
        selectionModel.clear();
        model.refresh();
        cellList.redraw();
    }

    /**
     * The Cell used to render a {@link Pid}.
     */
    static class PidCell extends AbstractCell<Pid> {

        @Override
        public void render(Context context, Pid pid, SafeHtmlBuilder safeHtmlBuilder) {
            // Value can be null, so do a null check..
            if (pid == null) {
                return;
            }

            safeHtmlBuilder.appendHtmlConstant("<table>");

            safeHtmlBuilder.appendHtmlConstant("<tr><td><span style='font-size:75%;'>");
            safeHtmlBuilder.appendEscaped(pid.toString());
            safeHtmlBuilder.appendHtmlConstant("</span>");
            // Add the name.
            if ( !("".equals(pid.getExtra())) ) {
                safeHtmlBuilder.appendHtmlConstant(" - ");
                safeHtmlBuilder.appendHtmlConstant("<span style='font-weight: bold;'>");
                safeHtmlBuilder.appendEscaped(pid.getExtra());
                safeHtmlBuilder.appendHtmlConstant("</span>");
            }
            safeHtmlBuilder.appendHtmlConstant("</td></tr></table>");
        }
    }


}
