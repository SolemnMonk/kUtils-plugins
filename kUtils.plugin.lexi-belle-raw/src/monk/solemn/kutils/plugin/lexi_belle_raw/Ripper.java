package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.text.MessageFormat;
import java.util.UUID;

import hall.caleb.seltzer.enums.SelectorType;
import hall.caleb.seltzer.objects.command.Command;
import hall.caleb.seltzer.objects.command.CommandFactory;
import hall.caleb.seltzer.objects.command.GoToCommand;
import hall.caleb.seltzer.objects.command.ReadAttributeCommand;
import hall.caleb.seltzer.objects.command.SelectorCommand;
import hall.caleb.seltzer.objects.command.WaitCommand;
import hall.caleb.seltzer.objects.response.MultiResultResponse;
import hall.caleb.seltzer.objects.response.SingleResultResponse;
import hall.caleb.seltzer.util.SeltzerUtils;
import monk.solemn.kutils.objects.QueuedTask;

public class Ripper {
	public static void performRip(UUID seleniumId, QueuedTask task) {
		String videoBaseUrl = LexiBelleRawPlugin.getUrl("VideosPage");
		String imageBaseUrl = LexiBelleRawPlugin.getUrl("ImageSetsPage");
		Command command;
		Integer page = 1;
		Integer results;
		
		command = new WaitCommand();
		((WaitCommand) command).setSeconds(30);
		((WaitCommand) command).setSelector(LexiBelleRawPlugin.getXpath("WelcomeBanner"), SelectorType.Xpath);
		SeltzerUtils.send((WaitCommand) command);
		
		do {
			command = CommandFactory.newGoToCommand(seleniumId, MessageFormat.format(videoBaseUrl, page));
			SeltzerUtils.send((GoToCommand) command);
			command = CommandFactory.newCountCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath("CardTitle"));
			results = Integer.parseInt(((SingleResultResponse) SeltzerUtils.send((SelectorCommand) command)).getResult());
			
			ripVideos(seleniumId, task);
			
			page++;
		} while (results == 10);
		
		page = 1;
		
		do {
			command = CommandFactory.newGoToCommand(seleniumId, MessageFormat.format(imageBaseUrl, page));
			SeltzerUtils.send((GoToCommand) command);
			command = CommandFactory.newCountCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath("CardTitle"));
			results = Integer.parseInt(((SingleResultResponse) SeltzerUtils.send((SelectorCommand) command)).getResult());
			
			ripPictures(seleniumId, task);
			
			page++;
		} while (results == 10);
	}

	private static void ripVideos(UUID seleniumId, QueuedTask task) {
		String titleXpath = LexiBelleRawPlugin.getXpath("CardTitle");
		ReadAttributeCommand command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, titleXpath, 0, "href");
		MultiResultResponse response = (MultiResultResponse) SeltzerUtils.send(command);
		
		for (String url : response.getResults()) {
			 Downloader.downloadVideo(seleniumId, task, url);
		}
	}

	private static void ripPictures(UUID seleniumId, QueuedTask task) {
		String titleXpath = LexiBelleRawPlugin.getXpath("CardTitle");
		ReadAttributeCommand command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, titleXpath, 0, "href");
		MultiResultResponse response = (MultiResultResponse) SeltzerUtils.send(command);
		
		for (String url : response.getResults()) {
			// Downloader.downloadPictureSet(seleniumId, task, url);
		}
	}
}
