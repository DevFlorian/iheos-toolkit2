package gov.nist.toolkit.xdstools2.client.inspector.mvp;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import gov.nist.toolkit.results.client.Result;
import gov.nist.toolkit.xdstools2.client.abstracts.AbstractToolkitActivity;
import gov.nist.toolkit.xdstools2.client.abstracts.ActivityDisplayer;
import gov.nist.toolkit.xdstools2.client.abstracts.GenericMVP;
import gov.nist.toolkit.xdstools2.client.injector.Injector;
import gov.nist.toolkit.xdstools2.client.util.ClientFactoryImpl;

/**
 *
 */
public class RegistryDbInspectorActivity extends AbstractToolkitActivity {

    private InspectorView view;
    private InspectorPresenter presenter;
    private ActivityDisplayer displayer;
    private RegistryDbInspector place;

    public RegistryDbInspectorActivity(RegistryDbInspector place) {
        super();
        this.place = place;
        GWT.log("Start activity ");
        if (place != null) {
            GWT.log("Build InspectorActivity for Place: " + place.getName());
        }
    }

    private GenericMVP<Result,InspectorView,InspectorPresenter> mvp;

    @Override
    public GenericMVP getMVP() {
        assert(mvp != null);
        return mvp;
    }

    @Override
    public LayoutPanel onResume() {
        return null;
    }

    @Override
    public void start(final AcceptsOneWidget acceptsOneWidget, final EventBus eventBus) {
        GWT.log("Starting RegistryDbInspector Activity - name is " + place.getName());
        presenter = Injector.INSTANCE.getInspectorPresenter();
        view =      Injector.INSTANCE.getInspectorView();
        displayer = Injector.INSTANCE.getToolkitAppDisplayer();
        assert(presenter != null);
        assert(displayer != null);

        presenter.setTitle("RegistryDbInspector");
        presenter.setDataModel(place.getMetadataCollection());
        presenter.setSiteSpec(place.getSiteSpec());
        finish(acceptsOneWidget, eventBus);
    }

    private void finish(AcceptsOneWidget acceptsOneWidget, EventBus eventBus) {
        mvp = buildMVP();
        mvp.init();

        presenter.setActivityDisplayer(displayer);  // so presenter can update tab title
        GWT.log("Calling activityDisplay");
        displayer.display(getContainer(), presenter.getTitle(), this, presenter, acceptsOneWidget, eventBus);


    }

    private GenericMVP<Result, InspectorView, InspectorPresenter> buildMVP() {
        assert(presenter != null);
        assert(view != null);
        return new GenericMVP<Result, InspectorView, InspectorPresenter>(view, presenter);
    }

    private Widget getContainer() {
        assert(mvp != null);
        return mvp.getDisplay();
    }

    public void goTo(Place place) {
        new ClientFactoryImpl().getPlaceController().goTo(place);
    }

}