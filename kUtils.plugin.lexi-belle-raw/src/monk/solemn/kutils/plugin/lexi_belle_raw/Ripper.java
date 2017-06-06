package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.text.MessageFormat;
import java.util.UUID;

import hall.caleb.selenium.enums.SelectorType;
import hall.caleb.selenium.objects.command.Command;
import hall.caleb.selenium.objects.command.CommandFactory;
import hall.caleb.selenium.objects.command.GoToCommand;
import hall.caleb.selenium.objects.command.ReadAttributeCommand;
import hall.caleb.selenium.objects.command.SelectorCommand;
import hall.caleb.selenium.objects.response.MultiResultResponse;
import hall.caleb.selenium.objects.response.SingleResultResponse;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;

public class Ripper {
	public static void performRip(UUID seleniumId, QueuedTask task) {
		String videoBaseUrl = LexiBelleRawPlugin.getUrl("VideosPage");
		String imageBaseUrl = LexiBelleRawPlugin.getUrl("VideosPage");
		Command command;
		Integer page = 1;
		Integer results;
		
		do {
			command = CommandFactory.newGoToCommand(seleniumId, MessageFormat.format(videoBaseUrl, page));
			SeleniumServerUtilities.sendSeleniumCommand((GoToCommand) command);
			command = CommandFactory.newCountCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath("CardTitle"));
			results = Integer.parseInt(((SingleResultResponse) SeleniumServerUtilities.sendSeleniumCommand((SelectorCommand) command)).getResult());
			
			ripVideos(seleniumId, task);
			
			page++;
		} while (results == 10);
		
		page = 1;
		
		do {
			command = CommandFactory.newGoToCommand(seleniumId, MessageFormat.format(imageBaseUrl, page));
			SeleniumServerUtilities.sendSeleniumCommand((GoToCommand) command);
			command = CommandFactory.newCountCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath("CardTitle"));
			results = Integer.parseInt(((SingleResultResponse) SeleniumServerUtilities.sendSeleniumCommand((SelectorCommand) command)).getResult());
			
			ripPictures(seleniumId, task);
			
			page++;
		} while (results == 10);
	}

	private static void ripVideos(UUID seleniumId, QueuedTask task) {
		String titleXpath = LexiBelleRawPlugin.getXpath("CardTitle");
		ReadAttributeCommand command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, titleXpath, 0, "href");
		MultiResultResponse response = (MultiResultResponse) SeleniumServerUtilities.sendSeleniumCommand(command);
		
		for (String url : response.getResults()) {
			 Downloader.downloadVideo(seleniumId, task, url);
		}
	}

	private static void ripPictures(UUID seleniumId, QueuedTask task) {
		String titleXpath = LexiBelleRawPlugin.getXpath("CardTitle");
		ReadAttributeCommand command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, titleXpath, 0, "href");
		MultiResultResponse response = (MultiResultResponse) SeleniumServerUtilities.sendSeleniumCommand(command);
		
		for (String url : response.getResults()) {
			// Downloader.downloadPictureSet(seleniumId, task, url);
		}
	}
}
