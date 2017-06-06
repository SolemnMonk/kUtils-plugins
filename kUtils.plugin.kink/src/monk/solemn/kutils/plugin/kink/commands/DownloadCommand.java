package monk.solemn.kutils.plugin.kink.commands;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.service.component.annotations.Component;

@Component(property={CommandProcessor.COMMAND_SCOPE + ":String=kutils",
		 			 CommandProcessor.COMMAND_FUNCTION + ":String=download"},
		   service=DownloadCommand.class)
public class DownloadCommand {

}
