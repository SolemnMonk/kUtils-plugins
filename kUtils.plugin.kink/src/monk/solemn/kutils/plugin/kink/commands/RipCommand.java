package monk.solemn.kutils.plugin.kink.commands;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import monk.solemn.kutils.api.base.NetworkBase;
import monk.solemn.kutils.api.base.PluginBase;
import monk.solemn.kutils.enums.Action;
import monk.solemn.kutils.enums.Target;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.objects.Task;

@Component(property={CommandProcessor.COMMAND_SCOPE + ":String=kutils",
					 CommandProcessor.COMMAND_FUNCTION + ":String=rip"},
		   service=RipCommand.class)
public class RipCommand {
	PluginBase kinkPlugin;
	
	@Reference
	void bindKinkPlugin(PluginBase kinkPlugin) {
		this.kinkPlugin = kinkPlugin;
	}
	
	public void rip() {
		Task task = new Task(Action.Rip, Target.Site);
		
		Map<String, String> options = new HashMap<>();
		options.put("site-friendly-name", "Kink Test Shoots");
		options.put("video-quality-label", "large");
		
		QueuedTask queuedTask = new QueuedTask(task, options);
		
		kinkPlugin.loadQueuedTask(queuedTask);
		
		((NetworkBase) kinkPlugin).run();
	}
}
