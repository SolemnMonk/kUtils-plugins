package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;

import monk.solemn.kutils.api.base.PluginBase;
import monk.solemn.kutils.api.base.SiteBase;
import monk.solemn.kutils.data.api.ActorDao;
import monk.solemn.kutils.data.api.ConfigDao;
import monk.solemn.kutils.data.api.CredentialDao;
import monk.solemn.kutils.data.api.FileStorageDao;
import monk.solemn.kutils.data.api.ShootDao;
import monk.solemn.kutils.enums.Action;
import monk.solemn.kutils.enums.ContentType;
import monk.solemn.kutils.enums.PluginType;
import monk.solemn.kutils.enums.Target;
import monk.solemn.kutils.objects.PluginInfo;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.objects.Task;
import monk.solemn.kutils.utilities.high.DaoUtilities;
import tech.seltzer.enums.CommandType;
import tech.seltzer.objects.command.CommandData;
import tech.seltzer.objects.exception.SeltzerException;
import tech.seltzer.util.SeltzerSend;

@Component
public class LexiBelleRawPlugin implements PluginBase, SiteBase {
	private static String pluginId = "lexi-belle-raw";
	
	private static ResourceBundle urls;
	private static ResourceBundle xpaths;
	private static ResourceBundle regex;
	
	private static QueuedTask queuedTask;
	private static PluginInfo info;

	private static UUID seltzerId;
	
	private static ActorDao actorDao;
	private static CredentialDao credentialDao;
	private static ShootDao shootDao;
	private static FileStorageDao fileStorageDao;
	private static ConfigDao configDao; 
	
	public LexiBelleRawPlugin() {
		initialize();
	}
	
	public void initialize() {
		System.out.println("Starting " + pluginId);

		loadProperties();
		
		actorDao = DaoUtilities.getActorDao();
		credentialDao = DaoUtilities.getCredentialDao();
		shootDao = DaoUtilities.getShootDao();
		fileStorageDao = DaoUtilities.getFileStorageDao();
		configDao = DaoUtilities.getConfigDao();
		
		try {
			seltzerId = SeltzerSend.send(new CommandData(CommandType.START)).getId();
		} catch (SeltzerException e) {
			e.printStackTrace();
		}
		
		System.out.println(pluginId + " started");
	}
	
	public void stop() {
		System.out.println("Stopping " + pluginId);
		
		unloadProperties();
		
		System.out.println(pluginId + " stopped");
	}
	
	@Override
	public void run() {
		if (taskRequiresAuthentication()) {
			new LexiBelleRawAuthentication().login();
		}
		
		if (queuedTask.getTask().getAction() == Action.Rip) {
			Ripper.performRip(seltzerId, queuedTask);
//		} else if (queuedTask.getTask().getAction() == Action.Download) {
//			Downloader.performDownload(seleniumId, queuedTask);
//		} else if (queuedTask.getTask().getAction() == Action.GatherData) {
//			DataGatherer.gatherData(seleniumId, queuedTask);
//		} else if (queuedTask.getTask().getAction() == Action.Monitor) {
//			Monitor.check(seleniumId, queuedTask);
		}
		
		if (taskRequiresAuthentication()) {
			new LexiBelleRawAuthentication().logout();
		}
		
		try {
			SeltzerSend.send(new CommandData(CommandType.EXIT, seltzerId));
		} catch (SeltzerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getSite() {
		return "Lexi Belle Raw";
	}

	@Override
	public boolean taskRequiresAuthentication() {
		return true;
	}

	@Override
	public PluginInfo getPluginInfo() {
		if (info == null) {
			info = new PluginInfo();

			info.setName("Lexi Belle Raw");
			info.setType(PluginType.Site);
			info.setDescription("A plugin for ripping content from Lexi Belle Raw.");
			info.setVersion("0.1.0");

			List<ContentType> contentTypes = new LinkedList<>();
			contentTypes.add(ContentType.Images);
			contentTypes.add(ContentType.Videos);

			info.setContentTypes(contentTypes);

			List<Task> tasks = new LinkedList<>();
			tasks.add(new Task(Action.Rip, Target.Site));
			tasks.add(new Task(Action.Download, Target.Shoot));
			tasks.add(new Task(Action.GatherData, Target.Shoot));
			tasks.add(new Task(Action.Monitor, Target.Site));
			
			info.setTasks(tasks);
		}
		
		return info;
	}

	@Override
	public boolean loadQueuedTask(QueuedTask queuedTask) {
		if (queuedTask == null) {
			return false;
		} else {
			LexiBelleRawPlugin.queuedTask = queuedTask;
			return true;
		}
	}
	
	private void loadProperties() {
		unloadProperties();

		urls = ResourceBundle.getBundle("urls");
		xpaths = ResourceBundle.getBundle("xpaths");
		regex = ResourceBundle.getBundle("regex");
	}
	
	private void unloadProperties() {
		urls = null;
		xpaths = null;
		regex = null;
	}
	
	public static String getUrl(String key) {
		if (urls.containsKey(key)) {
			return urls.getString(key);
		} else {
			return null;
		}
	}

	public static String getXpath(String key) {
		if (xpaths.containsKey(key)) {
			return xpaths.getString(key);
		} else {
			return null;
		}
	}
	
	public static String getRegex(String key) {
		if (regex.containsKey(key)) {
			return regex.getString(key);
		} else {
			return null;
		}
	}

	public static QueuedTask getQueuedTask() {
		return queuedTask;
	}
	
	public static ActorDao getActorDao() {
		return actorDao;
	}

	public static CredentialDao getCredentialDao() {
		return credentialDao;
	}

	public static ShootDao getShootDao() {
		return shootDao;
	}
	
	public static FileStorageDao getFileStorageDao() {
		return fileStorageDao;
	}

	public static ConfigDao getConfigDao() {
		return configDao;
	}

	public static UUID getSeltzerId() {
		return seltzerId;
	}

	public static void setSeltzerId(UUID seleniumId) {
		LexiBelleRawPlugin.seltzerId = seleniumId;
	}

	@Override
	public void install() {
		// TODO Auto-generated method stub
		
	}
}
