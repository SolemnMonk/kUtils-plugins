package monk.solemn.kutils.plugin.kink;

import java.util.List;
import java.util.UUID;

import hall.caleb.selenium.objects.command.CommandFactory;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;

public class Ripper {
	public static void performRip(UUID seleniumId, QueuedTask task) {
		boolean siteHasMoreShoots = true;
		int shootsOnPage = 0;
		List<String> shootLinks;
		
		for (int page = 1; siteHasMoreShoots; page++) {
			KinkUtilities.navigateToShootPage(seleniumId, task, page);
			shootLinks = KinkUtilities.getShootLinks(seleniumId);
			shootsOnPage = shootLinks.size();

			if (shootsOnPage == 0) {
				siteHasMoreShoots = false;
				continue;
			} else if (shootsOnPage == 20) {
				siteHasMoreShoots = true;
			} else {
				siteHasMoreShoots = false;
			}

			for (String link : shootLinks) {
				ripShoot(seleniumId, link, task);
				SeleniumServerUtilities.sendSeleniumCommand(CommandFactory.newBackCommand(seleniumId));
			}
		 }
	}

	private static void ripShoot(UUID seleniumId, String url, QueuedTask task) {
		Downloader.downloadShoot(seleniumId, url, task);
	}
}
