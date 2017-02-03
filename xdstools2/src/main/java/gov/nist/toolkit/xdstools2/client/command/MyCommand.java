package gov.nist.toolkit.xdstools2.client.command;

import gov.nist.toolkit.xdstools2.client.command.command.GetAssigningAuthoritiesCommand;
import gov.nist.toolkit.xdstools2.shared.command.CommandContext;

import java.util.List;

/**
 * Example Command to server
 */
public class MyCommand {

    public void doit() throws Exception {
        new GetAssigningAuthoritiesCommand() {

            // Completion callback
            @Override
            public void onComplete(List<String> var1) { // result type

            }
            @Override  // optional - default implementation in CommandModule
            public void onFailure(Throwable throwable) {}
        }.run(new CommandContext());   // request type

    }
}
