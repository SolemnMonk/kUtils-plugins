package monk.solemn.kutils.plugin.kink;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;

import hall.caleb.selenium.objects.command.CommandFactory;
import monk.solemn.kutils.api.base.NetworkBase;
import monk.solemn.kutils.api.base.PluginBase;
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
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;

@Component
public class KinkPlugin implements PluginBase, NetworkBase {
	private static String pluginId = "kink.com";
	
	private static BundleContext osgiContext;
	
	private static ResourceBundle urls;
	private static ResourceBundle xpaths;

	private static Map<String, String> sites;

	private static QueuedTask queuedTask;
	private static PluginInfo info;

	private static UUID seleniumId;
	
	private static ActorDao actorDao;
	private static CredentialDao credentialDao;
	private static ShootDao shootDao;
	private static FileStorageDao fileStorageDao;
	private static ConfigDao configDao; 

	public KinkPlugin() {
		initialize();
	}
	
	public void initialize() {
		System.out.println("Starting " + pluginId);

		loadSites();
		loadProperties();
		
		actorDao = DaoUtilities.getActorDao();
		credentialDao = DaoUtilities.getCredentialDao();
		shootDao = DaoUtilities.getShootDao();
		fileStorageDao = DaoUtilities.getFileStorageDao();
		configDao = DaoUtilities.getConfigDao();
		
		seleniumId = SeleniumServerUtilities.sendSeleniumCommand(CommandFactory.newStartCommand()).getId();
		
		System.out.println(pluginId + " started");
	}

	public void stop() {
		System.out.println("Stopping " + pluginId);
		
		unloadProperties();
		
		System.out.println(pluginId + " stopped");
	}

	@Override
	public PluginInfo getPluginInfo() {
		if (info == null) {
			info = new PluginInfo();

			info.setName("Kink.com");
			info.setType(PluginType.Network);
			info.setDescription("A plugin for ripping and gathering the data for shoots on the Kink.com network.");
			info.setVersion("0.0.1");

			List<ContentType> contentTypes = new LinkedList<>();
			contentTypes.add(ContentType.Images);
			contentTypes.add(ContentType.Videos);

			info.setContentTypes(contentTypes);

			List<Task> tasks = new LinkedList<>();
			tasks.add(new Task(Action.Rip, Target.Network));
			tasks.add(new Task(Action.Rip, Target.Site));
			tasks.add(new Task(Action.Rip, Target.Actor));
			tasks.add(new Task(Action.Download, Target.Series));
			tasks.add(new Task(Action.Download, Target.Shoot));
			tasks.add(new Task(Action.GatherData, Target.Series));
			tasks.add(new Task(Action.GatherData, Target.Shoot));
			tasks.add(new Task(Action.GatherData, Target.Actor));

			info.setTasks(tasks);
		}

		return info;
	}

	@Override
	public boolean loadQueuedTask(QueuedTask queuedTask) {
		if (queuedTask == null) {
			return false;
		} else {
			KinkPlugin.queuedTask = queuedTask;
			return true;
		}
	}

	@Override
	public List<String> getSites() {
		List<String> siteList = new LinkedList<>();

		for (String site : sites.keySet()) {
			siteList.add(site);
		}

		return siteList;
	}

	private void loadProperties() {
		unloadProperties();

		urls = ResourceBundle.getBundle("urls");
		xpaths = ResourceBundle.getBundle("xpaths");
	}

	private void unloadProperties() {
		urls = null;
		xpaths = null;
	}

	private void loadSites() {
		sites = new HashMap<>();

		sites.put("30 Minutes of Torment", "30minutesoftorment");
		sites.put("Animated Kink", "animatedkink");
		sites.put("Ball Gaggers", "ballgaggers");
		sites.put("Bleu", "bleufilms");
		sites.put("Bound Gang Bangs", "boundgangbangs");
		sites.put("Bound Gods", "boundgods");
		sites.put("Bound in Public", "boundinpublic");
		sites.put("Bound Men Wanked", "boundmenwanked");
		sites.put("Butt Machine Boys", "buttmachineboys");
		sites.put("Captive Male", "captivemale");
		sites.put("Chanta's Bitches", "chantasbitches");
		sites.put("Device Bondage", "devicebondage");
		sites.put("Divine Bitches", "divinebitches");
		sites.put("Dungeon Sex", "dungeonsex");
		sites.put("Electro Sluts", "electrosluts");
		sites.put("Everything Butt", "everythingbutt");
		sites.put("Foot Worship", "footworship");
		sites.put("Fucked And Bound", "fuckedandbound");
		sites.put("Fucking Machines", "fuckingmachines");
		sites.put("Gentlemen's Closet", "gentlemenscloset");
		sites.put("Hardcore Gangbang", "hardcoregangbang");
		sites.put("Hogtied Up", "hogtiedup");
		sites.put("Hogtied", "hogtied");
		sites.put("Kink Raw Test Shoots", "kinkrawtestshoots");
		sites.put("Kink Test Shoots", "kinktestshoots");
		sites.put("Kink University", "kinkuniversity");
		sites.put("KinkMen Test Shoots", "kinkmentestshoots");
		sites.put("Machine Dom", "machinedom");
		sites.put("Men in Pain", "meninpain");
		sites.put("Men on Edge", "menonedge");
		sites.put("My Friends' Feet", "myfriendsfeet");
		sites.put("Naked Kombat", "nakedkombat");
		sites.put("Nasty Daddy", "nastydaddy");
		sites.put("Public Disgrace", "publicdisgrace");
		sites.put("Sadistic Rope", "sadisticrope");
		sites.put("Sex and Submission", "sexandsubmission");
		sites.put("Struggling Babes", "strugglingbabes");
		sites.put("The Training of O", "thetrainingofo");
		sites.put("The Upper Floor", "theupperfloor");
		sites.put("TS Pussy Hunters", "tspussyhunters");
		sites.put("TS Seduction", "tsseduction");
		sites.put("Ultimate Surrender", "ultimatesurrender");
		sites.put("Water Bondage", "waterbondage");
		sites.put("Whipped Ass", "whippedass");
		sites.put("Wired Pussy", "wiredpussy");
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

	public static String getSiteShortName(String key) {
		if (sites.containsKey(key)) {
			return sites.get(key);
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
	
	public static BundleContext getOsgiContext() {
		return osgiContext;
	}

	public static void setOsgiContext(BundleContext osgiContext) {
		KinkPlugin.osgiContext = osgiContext;
	}

	@Override
	public void run() {
		if (taskRequiresAuthentication()) {
			new KinkAuthentication().login(seleniumId);
		}
		
		if (queuedTask.getTask().getAction() == Action.Rip) {
			Ripper.performRip(seleniumId, queuedTask);
		} else if (queuedTask.getTask().getAction() == Action.Download) {
			Downloader.performDownload(seleniumId, queuedTask);
		} else if (queuedTask.getTask().getAction() == Action.GatherData) {
			DataGatherer.gatherData(seleniumId, queuedTask);
		}
		
		if (taskRequiresAuthentication()) {
			new KinkAuthentication().logout(seleniumId);
		}
	}

	@Override
	public boolean taskRequiresAuthentication() {
		if (queuedTask.getTask().getAction() == Action.GatherData) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void install() {
		// TODO Auto-generated method stub
		
	}
}
