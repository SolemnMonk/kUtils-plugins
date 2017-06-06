package monk.solemn.kutils.plugin.kink;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;

import hall.caleb.selenium.enums.SeleniumCommandType;
import hall.caleb.selenium.enums.SeleniumSelectorType;
import hall.caleb.selenium.objects.SeleniumCommand;
import hall.caleb.selenium.objects.SeleniumResponse;
import monk.solemn.kutils.enums.ShootType;
import monk.solemn.kutils.objects.Actor;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.objects.Shoot;
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;
import monk.solemn.kutils.utilities.high.ShootUtilities;
import monk.solemn.kutils.utilities.low.StringUtilitiesLow;

public class Downloader {
	public static Shoot performDownload(UUID seleniumId, QueuedTask task) {
		return downloadShoot(seleniumId, task.getData().get("download-shoot-url"), task);
	}
	
	public static Shoot downloadShoot(UUID seleniumId, String url, QueuedTask task) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.GoTo);
		command.setUrl(url);
		SeleniumServerUtilities.sendSeleniumCommand(command);

		command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
		command.setSelector(KinkPlugin.getXpath("ShootTitle"), SeleniumSelectorType.Xpath);
		String title = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
		
		Shoot shoot;
		try {
			shoot = ShootUtilities.getShootByTitle(title);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}

		DataGatherer.getShootInfo(seleniumId, shoot, task);
		DataGatherer.getShootTags(seleniumId, shoot);
		DataGatherer.getShootRating(seleniumId, shoot);
		DataGatherer.getActors(seleniumId, shoot);
		DataGatherer.getShootCoverImage(seleniumId, shoot);
		DataGatherer.getShootPreviewImages(seleniumId, shoot);

		try {
			KinkPlugin.getShootDao().saveShoot(shoot);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		String sanitizedTitle = StringUtilitiesLow.sanitizeForPathName(shoot.getTitle());
		
		downloadTrailer(seleniumId, shoot, sanitizedTitle);

		if (shoot.getShootType() == ShootType.Video || shoot.getShootType() == ShootType.VideoAndImages) {
			downloadVideo(seleniumId, shoot, sanitizedTitle, task);
		}

		if (shoot.getShootType() == ShootType.Images || shoot.getShootType() == ShootType.VideoAndImages) {
			downloadImageSet(seleniumId, shoot, sanitizedTitle);
		}

		shoot.unloadImages();

		return shoot;
	}

	public static void downloadTrailer(UUID seleniumId, Shoot shoot, String sanitizedName) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(KinkPlugin.getXpath("TrailerDownloadButton"), SeleniumSelectorType.Xpath);
		int trailerButtonCount = SeleniumServerUtilities.sendSeleniumCommand(command).getCount();
		
		if (trailerButtonCount > 0) {
			command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadAttribute);
			command.setSelector(KinkPlugin.getXpath("TrailerSource"), SeleniumSelectorType.Xpath);
			command.setAttribute("data-url");

			String source = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
			
			String date = KinkUtilities.formatDate(shoot.getDate().getTime());
			String path = Paths.get("Shoots", date + " - " + sanitizedName).toString();

			File trailer = null;

			try {
				trailer = KinkPlugin.getFileStorageDao().downloadFile(source, path, true);
				trailer.renameTo(Paths
						.get(trailer.getParent(), date + " - " + sanitizedName + "-trailer." + FilenameUtils.getExtension(trailer.getName())).toFile());
			} catch (SQLException | IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void downloadVideo(UUID seleniumId, Shoot shoot, String sanitizedName, QueuedTask task) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(KinkPlugin.getXpath("MovieFullDownloadLinks"), SeleniumSelectorType.Xpath);
		
		int fullMovieDownloadButtonCount = SeleniumServerUtilities.sendSeleniumCommand(command).getCount(); 

		String date = KinkUtilities.formatDate(shoot.getDate().getTime());

		Map<String, String> metadataMap = new HashMap<>();
		
		StringBuilder actors = new StringBuilder();
		for (Actor actor : shoot.getActors()) {
			actors.append(actor.getName());
			actors.append('/');
		}
		
		metadataMap.put("album", task.getData().get("site-friendly-name"));
		metadataMap.put("album_artist", "Kink.com");
		metadataMap.put("artist", actors.toString().substring(0, actors.toString().length() - 1));
		metadataMap.put("composer", "Kink.com");
		metadataMap.put("POPM", ((Integer) ((Double) (shoot.getRating().getAvgRating() * 255)).intValue()).toString());
		metadataMap.put("TDAT", new SimpleDateFormat("ddMM").format(shoot.getDate().getTime()));
		metadataMap.put("TIT2", shoot.getDescription());		
		metadataMap.put("title", shoot.getTitle());
		metadataMap.put("date", new SimpleDateFormat("yyyy").format(shoot.getDate().getTime()));

		SeleniumCommand subCommand;
		
		if (fullMovieDownloadButtonCount > 0) {
			command = new SeleniumCommand(seleniumId, SeleniumCommandType.Click);
			command.setSelector(KinkPlugin.getXpath("MovieFullDownloadButton"), SeleniumSelectorType.Xpath);
			SeleniumServerUtilities.sendSeleniumCommand(command);

			String qualityLabel = task.getData().get("video-quality-label");
			String linkText;

			String downloadLink = null;
			List<String> fullMovieDownloadLinks = new ArrayList<>();
			
			command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
			command.setSelector(KinkPlugin.getXpath("MovieFullDownloadLinks"), SeleniumSelectorType.Xpath);
			int fullMovieDownloadLinkCount = SeleniumServerUtilities.sendSeleniumCommand(command).getCount();
			
			String fullMovieDownloadLinkBase = KinkPlugin.getXpath("MovieFullDownloadLink");
			String url = null;
			
			for (Integer i = 1; i <= fullMovieDownloadLinkCount; i++) {
				command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
				command.setSelector(MessageFormat.format(fullMovieDownloadLinkBase, i.toString()), SeleniumSelectorType.Xpath);
				if (qualityLabel.equals(SeleniumServerUtilities.sendSeleniumCommand(command).getText().trim().toLowerCase())) {
					command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadAttribute);
					command.setAttribute("download/href");
					url = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
					break;
				}
			}
			
			String path = Paths.get("Shoots", date + " - " + sanitizedName).toString();

			File movie = null;
			
			try {
				movie = KinkPlugin.getFileStorageDao().downloadFile(url, path, true);
				movie.renameTo(
						Paths.get(movie.getParent(), sanitizedName + "." + FilenameUtils.getExtension(movie.getName()))
								.toFile());
				movie = Paths.get(movie.getParent(), sanitizedName + "." + FilenameUtils.getExtension(movie.getName())).toFile();
				movie = KinkPlugin.getFileStorageDao().applyMetadata(movie, metadataMap, null);
			} catch (SQLException | IOException | InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			String path = Paths.get("Shoots", date + " - " + sanitizedName).toString();

			command = new SeleniumCommand(seleniumId, SeleniumCommandType.Click);
			command.setSelector(KinkPlugin.getXpath("MoviePartsDownloadButton"), SeleniumSelectorType.Xpath);
			SeleniumServerUtilities.sendSeleniumCommand(command);

			command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
			command.setSelector(KinkPlugin.getXpath("MoviePartsSections"), SeleniumSelectorType.Xpath);
			int clipSectionCount = SeleniumServerUtilities.sendSeleniumCommand(command).getCount();
			
			String clipSectionBase = KinkPlugin.getXpath("MoviePartsSections");
			String clipSectionLinksBase = clipSectionBase + KinkPlugin.getXpath("a");
			
			String sectionName;
			SeleniumResponse response;
			SeleniumResponse subResponse;
			int linkCount;
			List<String> urls;
			
			for (Integer i = 1; i <= clipSectionCount; i++) {
				urls = new ArrayList<>();
				
				command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
				command.setSelector(MessageFormat.format(clipSectionBase, i.toString()), SeleniumSelectorType.Xpath);
				
				sectionName = SeleniumServerUtilities.sendSeleniumCommand(command).getText().trim().toUpperCase();
				
				command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
				command.setSelector(MessageFormat.format(clipSectionLinksBase, i.toString()), SeleniumSelectorType.Xpath);
				
				linkCount = SeleniumServerUtilities.sendSeleniumCommand(command).getCount();
				
				for (Integer j = 1; j <= linkCount; j++) {
					command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadAttribute);
					command.setSelector(MessageFormat.format(clipSectionLinksBase + "[" + j + "]", i.toString()), SeleniumSelectorType.Xpath);
					command.setAttribute("download/href");
					
					urls.add(SeleniumServerUtilities.sendSeleniumCommand(command).getText());
				}
				
				try {
					KinkPlugin.getFileStorageDao().downloadClips(urls, path, sectionName, sanitizedName, true, true,
							metadataMap);
				} catch (IOException | SQLException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void downloadImageSet(UUID seleniumId, Shoot shoot, String sanitizedName) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(KinkPlugin.getXpath("ImagesDownloadLinks"), SeleniumSelectorType.Xpath);
		int imageSetCount = SeleniumServerUtilities.sendSeleniumCommand(command).getCount();
		
		command = new SeleniumCommand(seleniumId, SeleniumCommandType.Chain);
		SeleniumCommand subCommand;
		
		String imageDownloadLinkBase = KinkPlugin.getXpath("ImagesDownloadLinks"); 
		
		for (Integer i = 1; i <= imageSetCount; i++) {
			subCommand = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadAttribute);
			subCommand.setSelector(MessageFormat.format(imageDownloadLinkBase, i.toString()), SeleniumSelectorType.Xpath);
			subCommand.setAttribute("download/href");
			command.getCommands().add(subCommand);
		}

		List<String> imageSetLinks = new ArrayList<>();
		SeleniumResponse response = SeleniumServerUtilities.sendSeleniumCommand(command);
		for (SeleniumResponse r : response.getResponses()) {
			imageSetLinks.add(r.getText());
		}
		
		String date = KinkUtilities.formatDate(shoot.getDate().getTime());
		String path = Paths.get("Shoots", date + " - " + sanitizedName).toString();

		try {
			KinkPlugin.getFileStorageDao().downloadArchives(imageSetLinks, path, sanitizedName, true);
		} catch (IOException | SQLException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
