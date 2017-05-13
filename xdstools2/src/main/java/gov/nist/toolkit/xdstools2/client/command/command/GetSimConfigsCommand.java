package gov.nist.toolkit.xdstools2.client.command.command;

import gov.nist.toolkit.actorfactory.client.SimulatorConfig;
import gov.nist.toolkit.xdstools2.client.initialization.XdsTools2Presenter;
import gov.nist.toolkit.xdstools2.shared.command.request.GetSimConfigsRequest;

import java.util.List;

/**
 * Created by onh2 on 11/7/16.
 */
public abstract class GetSimConfigsCommand extends GenericCommand<GetSimConfigsRequest,List<SimulatorConfig>>{
    @Override
    public void run(GetSimConfigsRequest var1) {
        XdsTools2Presenter.data().getToolkitServices().getSimConfigs(var1,this);
    }
}
