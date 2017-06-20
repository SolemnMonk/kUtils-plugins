package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.text.MessageFormat;
import java.util.UUID;

import hall.caleb.seltzer.enums.SelectorType;
import hall.caleb.seltzer.objects.command.Command;
import hall.caleb.seltzer.objects.command.GoToCommand;
import hall.caleb.seltzer.objects.command.ReadAttributeCommand;
import hall.caleb.seltzer.objects.command.SelectorCommand;
import hall.caleb.seltzer.objects.command.WaitCommand;
import hall.caleb.seltzer.objects.response.MultiResultResponse;
import hall.caleb.seltzer.objects.response.SingleResultResponse;
import hall.caleb.seltzer.util.CommandFactory;
import hall.caleb.seltzer.util.SeltzerUtils;
import monk.solemn.kutils.objects.QueuedTask;

public class Ripper {
	public static void performRip(UUID seleniumId, QueuedTask task) {
		String videoBaseUrl = LexiBelleRawPlugin.getUrl("VideosPage");
		String imageBaseUrl = LexiBelleRawPlugin.getUrl("ImageSetsPage");
		Command command;
		Integer page = 1;
		Integer results;
		
		command = new WaitCommand(seleniumId);
		((WaitCommand) command).setSeconds(30);
		((WaitCommand) command).setSelector(LexiBelleRawPlugin.getXpath("WelcomeBanner"), SelectorType.Xpath);
		SeltzerUtils.send((WaitCommand) command);

		String cardTitles = LexiBelleRawPlugin.getXpath("CardTitles");
		
		do {
			command = CommandFactory.newGoToCommand(seleniumId, MessageFormat.format(videoBaseUrl, page));
			SeltzerUtils.send((GoToCommand) command);
			
			waitForTitles(seleniumId);
			
			command = CommandFactory.newCountCommand(seleniumId, SelectorType.Xpath, cardTitles);
			results = Integer.parseInt(((SingleResultResponse) SeltzerUtils.send((SelectorCommand) command)).getResult());
			
			ripVideos(seleniumId, task, results);
			
			page++;
		} while (results == 10);
		
		page = 1;
		
		do {
			command = CommandFactory.newGoToCommand(seleniumId, MessageFormat.format(imageBaseUrl, page));
			SeltzerUtils.send((GoToCommand) command);
			
			waitForTitles(seleniumId);
			
			command = CommandFactory.newCountCommand(seleniumId, SelectorType.Xpath, cardTitles);
			results = Integer.parseInt(((SingleResultResponse) SeltzerUtils.send((SelectorCommand) command)).getResult());
			
			ripPictures(seleniumId, task, results);
			
			page++;
		} while (results == 10);
	}

	private static void ripVideos(UUID seleniumId, QueuedTask task, int videoCount) {
		String titleXpath = LexiBelleRawPlugin.getXpath("CardTitle");
		ReadAttributeCommand command;
		MultiResultResponse response;
		String url;
		
		for (int i = 1; i <= videoCount; i++) {
			waitForTitles(seleniumId);
			
			int column = (i % 2 == 1 ? 1 : 2);
			int index = (i + 1) / 2;
			
			command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, MessageFormat.format(titleXpath, column, index), 1, "href");
			response = (MultiResultResponse) SeltzerUtils.send(command);
			url = response.getResults().get(0);
			
			DataGatherer.cacheCoverImageUrl(seleniumId, column, index);
			Downloader.downloadVideo(seleniumId, task, url);
			SeltzerUtils.send(CommandFactory.newBackCommand(seleniumId));
		}
	}

	private static void ripPictures(UUID seleniumId, QueuedTask task, int albumCount) {
		String titleXpath = LexiBelleRawPlugin.getXpath("CardTitle");
		ReadAttributeCommand command;
		MultiResultResponse response;
		String url;
		
		for (int i = 1; i <= albumCount; i++) {
			waitForTitles(seleniumId);
			
			int column = (i % 2 == 1 ? 1 : 2);
			int index = (i + 1) / 2;
			
			command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, MessageFormat.format(titleXpath, column, index), 1, "href");
			response = (MultiResultResponse) SeltzerUtils.send(command);
			url = response.getResults().get(0);
			
			DataGatherer.cacheCoverImageUrl(seleniumId, column, index);
			Downloader.downloadImageSet(seleniumId, task, url);
			SeltzerUtils.send(CommandFactory.newBackCommand(seleniumId));
		}
	}
	
	private static void waitForTitles(UUID seleniumId) {
		WaitCommand command = new WaitCommand(seleniumId);
		command.setSeconds(30);
		command.setSelector(LexiBelleRawPlugin.getXpath("CardTitles"), SelectorType.Xpath);
		SeltzerUtils.send((WaitCommand) command);
	}
}
