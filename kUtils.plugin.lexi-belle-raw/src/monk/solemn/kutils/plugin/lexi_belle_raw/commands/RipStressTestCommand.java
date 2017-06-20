package monk.solemn.kutils.plugin.lexi_belle_raw.commands;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import monk.solemn.kutils.api.base.PluginBase;
import monk.solemn.kutils.api.base.SiteBase;
import monk.solemn.kutils.enums.Action;
import monk.solemn.kutils.enums.Target;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.objects.Task;
import monk.solemn.kutils.utilities.high.DaoUtilities;

@Component(property={CommandProcessor.COMMAND_SCOPE + ":String=lbr",
		 			 CommandProcessor.COMMAND_FUNCTION + ":String=ripStressTest"},
		   service=RipStressTestCommand.class)
public class RipStressTestCommand {
	PluginBase lbrPlugin;
	
	@Reference
	void bindLbrPlugin(PluginBase lbrPlugin) {
		this.lbrPlugin = lbrPlugin;
	}
	
	public void ripStressTest() {
		Task task = new Task(Action.Rip, Target.Site);
		
		Map<String, String> options = new HashMap<>();
		
		QueuedTask queuedTask = new QueuedTask(task, options);
		
		lbrPlugin.loadQueuedTask(queuedTask);
		
		String basePath = ".";
		try {
			basePath = DaoUtilities.getConfigDao().loadConfig("basePath");
			if (StringUtils.isEmpty(basePath)) {
				throw (new RuntimeException("No base path loaded!"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
			return;
		}
		
		for (int i = 0; i < 10; i++) {
			((SiteBase) lbrPlugin).run();
			try {
				FileUtils.deleteDirectory(Paths.get(basePath, "Lexi Belle Raw").toFile());
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
	}
}
