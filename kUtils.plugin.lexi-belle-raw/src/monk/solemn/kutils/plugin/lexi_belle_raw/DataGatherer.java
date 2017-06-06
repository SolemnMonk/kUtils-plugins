package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.util.UUID;

import hall.caleb.selenium.enums.SelectorType;
import hall.caleb.selenium.objects.command.CommandFactory;
import hall.caleb.selenium.objects.response.MultiResultResponse;
import monk.solemn.kutils.objects.Rating;
import monk.solemn.kutils.objects.Shoot;
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;

public class DataGatherer {
	public static void getShootDescription(UUID seleniumId, Shoot shoot) {
		String xpath = LexiBelleRawPlugin.getXpath("ShootDescription");
		MultiResultResponse response;
		response = (MultiResultResponse) SeleniumServerUtilities.sendSeleniumCommand(CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, xpath, 1));
		
		shoot.setDescription(response.getResults().get(0));
	}
	
	public static void getShootCoverImage(UUID seleniumId, Shoot shoot) {
		SeleniumServerUtilities.sendSeleniumCommand(CommandFactory.newBackCommand(seleniumId));
		
		// Logic here
		
		SeleniumServerUtilities.sendSeleniumCommand(CommandFactory.newForwardCommand(seleniumId));
	}
	
	public static void getShootRating(UUID seleniumId, Shoot shoot) {
		String upXpath = LexiBelleRawPlugin.getXpath("ThumbsUp");
		String downXpath = LexiBelleRawPlugin.getXpath("ThumbsDown");
		
		Integer thumbsUp;
		Integer thumbsDown;
		
		MultiResultResponse response;
		
		response = (MultiResultResponse) SeleniumServerUtilities.sendSeleniumCommand(CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, upXpath, 1));
		thumbsUp = Integer.parseInt(response.getResults().get(0));
		response = (MultiResultResponse) SeleniumServerUtilities.sendSeleniumCommand(CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, downXpath, 1));
		thumbsDown = Integer.parseInt(response.getResults().get(0));
		
		Integer sum = thumbsUp + thumbsDown;
		
		shoot.setRating(new Rating(thumbsUp.doubleValue() / sum, sum));
	}
}
