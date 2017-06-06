package monk.solemn.kutils.plugin.kink;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import hall.caleb.selenium.enums.SelectorType;
import hall.caleb.selenium.objects.command.CommandFactory;
import hall.caleb.selenium.objects.command.GoToCommand;
import hall.caleb.selenium.objects.command.ReadAttributeCommand;
import hall.caleb.selenium.objects.response.MultiResultResponse;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;

public class KinkUtilities {
	public static void navigateToLatestShoots(UUID seleniumId, QueuedTask task) {
		navigateToShootPage(seleniumId, task, 1);
	}
	
	public static void navigateToShootPage(UUID seleniumId, QueuedTask task, Integer page) {
		String baseUrl = KinkPlugin.getUrl("ChannelShootPage");
		String channel = KinkPlugin.getSiteShortName(task.getData().get("site-friendly-name"));
		String channelUrl = MessageFormat.format(baseUrl, channel, page.toString());
		
		GoToCommand command = CommandFactory.newGoToCommand(seleniumId, channelUrl);
		SeleniumServerUtilities.sendSeleniumCommand(command);
	}
	
	public static List<String> getShootLinks(UUID seleniumId) {
		String shootsXpath = KinkPlugin.getXpath("AllShoots");

		ReadAttributeCommand command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, shootsXpath, 0, "href");
		
		MultiResultResponse response = (MultiResultResponse) SeleniumServerUtilities.sendSeleniumCommand(command);
		
		return response.getResults();
	}
	
	public static Date parseDate(String dateFormat, String rawDate) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat, Locale.US);
		
		Date date = new Date();
		try {
			date = dateFormatter.parse(rawDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return date;
	}
	
	public static String formatDate(Date date) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		return dateFormatter.format(date);
	}
	
	public static List<String> parseTags(String rawTags) {
		List<String> tags = new LinkedList<>();
		String[] tagArray = null;
		
		tagArray = rawTags.split(",");
		for (String tag : tagArray) {
			tags.add(StringUtils.capitalize(tag.trim()));
		}
		
		return tags;
	}
}
