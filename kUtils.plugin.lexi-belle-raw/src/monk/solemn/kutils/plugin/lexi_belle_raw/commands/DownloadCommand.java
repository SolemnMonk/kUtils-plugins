package monk.solemn.kutils.plugin.lexi_belle_raw.commands;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import monk.solemn.kutils.api.base.PluginBase;
import monk.solemn.kutils.enums.Action;
import monk.solemn.kutils.enums.Target;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.objects.Task;

@Component(property={CommandProcessor.COMMAND_SCOPE + ":String=lbr",
		 			 CommandProcessor.COMMAND_FUNCTION + ":String=rip"},
		   service=DownloadCommand.class)
public class DownloadCommand {
	PluginBase lbrPlugin;
	
	@Reference
	void bindLbrPlugin(PluginBase lbrPlugin) {
		this.lbrPlugin = lbrPlugin;
	}
	
	public void run(Target targetType, String url) {
		Task task = new Task(Action.DOWNLOAD, targetType);
		
		Map<String, String> options = new HashMap<>();
		options.put("url", url);
		
		QueuedTask queuedTask = new QueuedTask(task, options);
		
		lbrPlugin.loadQueuedTask(queuedTask);
	}
}
