package gov.nist.toolkit.xdstools2.client.command.command;

import gov.nist.toolkit.xdstools2.shared.command.CommandContext;
import gov.nist.toolkit.xdstools2.shared.command.InitializationResponse;
import gov.nist.toolkit.xdstools2.client.util.ClientUtils;


/**
 *
 */
public abstract class InitializationCommand extends GenericCommand<CommandContext, InitializationResponse> {
    @Override
    public void run(CommandContext var1) {
        ClientUtils.INSTANCE.getToolkitServices().getInitialization(var1,this);
    }
}
