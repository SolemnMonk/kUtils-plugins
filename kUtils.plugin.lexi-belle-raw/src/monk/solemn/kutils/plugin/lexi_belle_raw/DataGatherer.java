package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.util.UUID;

import hall.caleb.seltzer.enums.SelectorType;
import hall.caleb.seltzer.objects.command.CommandFactory;
import hall.caleb.seltzer.objects.response.MultiResultResponse;
import hall.caleb.seltzer.util.SeltzerUtils;
import monk.solemn.kutils.objects.Rating;
import monk.solemn.kutils.objects.Shoot;

public class DataGatherer {
	public static void getShootDescription(UUID seleniumId, Shoot shoot) {
		String xpath = LexiBelleRawPlugin.getXpath("ShootDescription");
		MultiResultResponse response;
		response = (MultiResultResponse) SeltzerUtils.send(CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, xpath, 1));
		
		shoot.setDescription(response.getResults().get(0));
	}
	
	public static void getShootCoverImage(UUID seleniumId, Shoot shoot) {
		SeltzerUtils.send(CommandFactory.newBackCommand(seleniumId));
		
		// Logic here
		
		SeltzerUtils.send(CommandFactory.newForwardCommand(seleniumId));
	}
	
	public static void getShootRating(UUID seleniumId, Shoot shoot) {
		String upXpath = LexiBelleRawPlugin.getXpath("ThumbsUp");
		String downXpath = LexiBelleRawPlugin.getXpath("ThumbsDown");
		
		Integer thumbsUp;
		Integer thumbsDown;
		
		MultiResultResponse response;
		
		response = (MultiResultResponse) SeltzerUtils.send(CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, upXpath, 1));
		thumbsUp = Integer.parseInt(response.getResults().get(0));
		response = (MultiResultResponse) SeltzerUtils.send(CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, downXpath, 1));
		thumbsDown = Integer.parseInt(response.getResults().get(0));
		
		Integer sum = thumbsUp + thumbsDown;
		
		shoot.setRating(new Rating(thumbsUp.doubleValue() / sum, sum));
	}
}
