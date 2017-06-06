package monk.solemn.kutils.plugin.kink;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import com.google.gson.Gson;

import hall.caleb.selenium.enums.SeleniumCommandType;
import hall.caleb.selenium.enums.SeleniumSelectorType;
import hall.caleb.selenium.objects.SeleniumCommand;
import monk.solemn.kutils.enums.ShootType;
import monk.solemn.kutils.objects.Actor;
import monk.solemn.kutils.objects.ActorAttribute;
import monk.solemn.kutils.objects.KUtilsImage;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.objects.Rating;
import monk.solemn.kutils.objects.Shoot;
import monk.solemn.kutils.utilities.high.ActorUtilities;
import monk.solemn.kutils.utilities.high.ImageUtilitiesHigh;
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;
import monk.solemn.kutils.utilities.high.ShootUtilities;
import monk.solemn.kutils.utilities.high.StringUtilitiesHigh;
import monk.solemn.kutils.utilities.low.ImageUtilitiesLow;
import monk.solemn.kutils.utilities.low.StringUtilitiesLow;

public class DataGatherer {
	public static Shoot gatherData(UUID seleniumId, QueuedTask task) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.GoTo);
		command.setUrl(task.getData().get("download-shoot-url"));
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
		
		return shoot;
	}
	
	public static void getShootInfo(UUID seleniumId, Shoot shoot, QueuedTask task) {
		shoot.setTitle(StringUtilitiesLow.normalizeString(shoot.getTitle()));
		
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.GetUrl);
		shoot.setExternalUrl(SeleniumServerUtilities.sendSeleniumCommand(command).getText());
		
		command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
		command.setSelector(KinkPlugin.getXpath("ShootDescription"), SeleniumSelectorType.Xpath);
		shoot.setDescription(SeleniumServerUtilities.sendSeleniumCommand(command).getText());
		shoot.setSite(task.getData().get("site-friendly-name"));
		
		getShootType(seleniumId, shoot);
		
		command.setSelector(KinkPlugin.getXpath("RawDate"));
		String rawDate = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
		rawDate = rawDate.substring(rawDate.lastIndexOf(':') + 1).trim();
		Calendar date = Calendar.getInstance();
		date.setTime(KinkUtilities.parseDate("MMMM d, yyyy", rawDate));
		shoot.setDate(date);
	}
	
	private static void getShootType(UUID seleniumId, Shoot shoot) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(KinkPlugin.getXpath("ImagesDownloadLinks"), SeleniumSelectorType.Xpath);
		boolean imagesPresent = SeleniumServerUtilities.sendSeleniumCommand(command).getCount() > 0;
		
		command.setSelector(KinkPlugin.getXpath("MoviePartsDownloadButton"));
		boolean videosPresent = SeleniumServerUtilities.sendSeleniumCommand(command).getCount() > 0;
		
		command.setSelector(KinkPlugin.getXpath("MovieFullDownloadButton"));
		videosPresent = videosPresent || SeleniumServerUtilities.sendSeleniumCommand(command).getCount() > 0;
		
		if (imagesPresent && videosPresent) {
			shoot.setShootType(ShootType.VideoAndImages);
		} else if (imagesPresent) {
			shoot.setShootType(ShootType.Images);
		} else if (videosPresent) {
			shoot.setShootType(ShootType.Video);
		} else {
			shoot.setShootType(ShootType.Unknown);
		}
	}

	public static void getShootTags(UUID seleniumId, Shoot shoot) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
		command.setSelector(KinkPlugin.getXpath("RawTags"), SeleniumSelectorType.Xpath);
		String rawTags = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
		shoot.setTags(KinkUtilities.parseTags(rawTags));
	}

	public static void getShootRating(UUID seleniumId, Shoot shoot) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.GetUrl);
		String url = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
		String shootId = url.substring(url.lastIndexOf('/') + 1);
		String baseUrl = KinkPlugin.getUrl("Rating");
		url = MessageFormat.format(baseUrl, shootId.toString());
		command = new SeleniumCommand(seleniumId, SeleniumCommandType.GoTo);
		command.setUrl(url);
		SeleniumServerUtilities.sendSeleniumCommand(command);
		
		command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
		command.setSelector(KinkPlugin.getXpath("RatingJson"), SeleniumSelectorType.Xpath);
		String json = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
		Rating rating;
		if (json.contains("unrated")) {
			rating = new Rating(0.0, 0);
		} else {
			rating = (new Gson()).fromJson(json, Rating.class);
		}
		rating.setAvgRating(rating.getAvgRating() / 5);
		shoot.setRating(rating);
		
		command = new SeleniumCommand(seleniumId, SeleniumCommandType.Back);
		SeleniumServerUtilities.sendSeleniumCommand(command).getText();
	}

	public static void getShootCoverImage(UUID seleniumId, Shoot shoot) {
		KUtilsImage coverImage = shoot.getCoverImage();
		
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(KinkPlugin.getXpath("VideoPlayer"), SeleniumSelectorType.Xpath);
		boolean playerFound = SeleniumServerUtilities.sendSeleniumCommand(command).getCount() == 1;
		String source = "";
		try {
			if (playerFound) {
				command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadAttribute);
				command.setSelector(KinkPlugin.getXpath("VideoPlayer"), SeleniumSelectorType.Xpath);
				command.setAttribute("style");
				String style = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
				style = style.substring(style.indexOf("background-image")).trim();
				style = style.substring(style.indexOf("url(\""));
				if (style.contains(";")) {
					style = style.substring(0, style.indexOf(';')).trim();
				}
				source = style.substring(5, style.length() - 2);
			} else {
				command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
				command.setSelector(KinkPlugin.getXpath("VideoPlayerPlaceholder"), SeleniumSelectorType.Xpath);
				boolean coverAlternateFound = SeleniumServerUtilities.sendSeleniumCommand(command).getCount() == 1;
				if (coverAlternateFound) {
					command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadAttribute);
					command.setSelector(KinkPlugin.getXpath("VideoPlayerPlaceholder"), SeleniumSelectorType.Xpath);
					command.setAttribute("src");
					source = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
				}
			}
			
			if (StringUtils.isNotBlank(source)) {
				URL sourceUrl = new URL(source);
				BufferedImage imageBuffer = ImageIO.read(sourceUrl);
				
				if (coverImage == null) {
					String date = KinkUtilities.formatDate(shoot.getDate().getTime());
					String name = StringUtilitiesLow.sanitizeForPathName(shoot.getTitle());
					coverImage = new KUtilsImage(Paths.get("Shoots", date + " - " + name, "poster.png").toString());
				}
				
				String bufferHash = null;
				if (coverImage.getImage() != null) {
					bufferHash = ImageUtilitiesLow.HashImage((RenderedImage) coverImage.getImage());
				} else {
					bufferHash = "";
				}
				
				if (StringUtils.isEmpty(coverImage.getHash()) || !coverImage.getHash().equals(bufferHash)) {
					coverImage.setImage(imageBuffer);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		shoot.setCoverImage(coverImage);
	}
	
	public static void getShootPreviewImages(UUID seleniumId, Shoot shoot) {
		String kinkLargeImageFileNameAttributeName = "data-image-file";
		String kinkLargeImageFilePathAttributeName = "data-src";
		
		List<KUtilsImage> previewImages = new LinkedList<KUtilsImage>();
		previewImages.addAll(shoot.getPreviewImages());
		
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(KinkPlugin.getXpath("PreviewImageList"), SeleniumSelectorType.Xpath);
		int imageElementCount = SeleniumServerUtilities.sendSeleniumCommand(command).getCount();
		
		String source;
		URL sourceUrl;
		String largeImageFile;
		String largeImagePath;
		BufferedImage imageBuffer = null;
		KUtilsImage image;
		for (Integer i = 1; i <= imageElementCount; i++) {
			command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadAttribute);
			command.setSelector(KinkPlugin.getXpath("PreviewImageList") + "[" + i + "]", SeleniumSelectorType.Xpath);
			command.setAttribute(kinkLargeImageFileNameAttributeName);
			largeImageFile = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
			
			command.setSelector(KinkPlugin.getXpath("PreviewImageDialog"));
			command.setAttribute(kinkLargeImageFilePathAttributeName);
			largeImagePath = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
			
			source = largeImagePath + largeImageFile;
			try {
				if (StringUtils.isNotBlank(source)) {
					sourceUrl = new URL(source);
					imageBuffer = ImageIO.read(sourceUrl);
					
					if (!ImageUtilitiesHigh.imageExists(imageBuffer, shoot.getPreviewImages())) {
						String date = KinkUtilities.formatDate(shoot.getDate().getTime());
						String name = StringUtilitiesLow.sanitizeForPathName(shoot.getTitle());
						image = new KUtilsImage(Paths.get("Shoots", date + " - " + name, "preview-" + (previewImages.size() + 1)+ ".png").toString());
						image.setImage(imageBuffer);
						previewImages.add(image);
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		shoot.setPreviewImages(previewImages);
	}

	public static void getActors(UUID seleniumId, Shoot shoot) {
		String fallbackNameName;
		String actorUrl;
		
		String actorGroupXpath = KinkPlugin.getXpath("ActorGroup");
		if (shoot.getActors() == null) {
			shoot.setActors(new ArrayList<>());
		}
		
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(actorGroupXpath, SeleniumSelectorType.Xpath);
		int numActors = SeleniumServerUtilities.sendSeleniumCommand(command).getCount();
		
		for (Integer i = 1; i <= numActors; i++) {
			command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
			command.setSelector(actorGroupXpath + "[" + i + "]", SeleniumSelectorType.Xpath);
			fallbackNameName = SeleniumServerUtilities.sendSeleniumCommand(command).getText().trim();
			
			command.setCommandType(SeleniumCommandType.ReadAttribute);
			command.setAttribute("href");
			actorUrl = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
			
			command = new SeleniumCommand(seleniumId, SeleniumCommandType.GoTo);
			command.setUrl(actorUrl);
			SeleniumServerUtilities.sendSeleniumCommand(command);
			
			try {
				shoot.getActors().add(getActorData(seleniumId, shoot, fallbackNameName));
			} catch (SQLException e) {
				e.printStackTrace();
			}

			command = new SeleniumCommand(seleniumId, SeleniumCommandType.Back);
			SeleniumServerUtilities.sendSeleniumCommand(command);
		}
	}
	
	public static Actor getActorData(UUID seleniumId, Shoot shoot, String fallbackName) throws SQLException {
		Actor actor = ActorUtilities.getActorByName(fallbackName);
		
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.GetUrl);
		String currentUrl = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
		
		command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(KinkPlugin.getXpath("ActorFound"), SeleniumSelectorType.Xpath);
		boolean actorFoundPage = SeleniumServerUtilities.sendSeleniumCommand(command).getCount() == 1;
		if (actorFoundPage) {
			command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
			command.setSelector(KinkPlugin.getXpath("ModelName"), SeleniumSelectorType.Xpath);
			String name = SeleniumServerUtilities.sendSeleniumCommand(command).getText().trim();
			actor = ActorUtilities.getActorByName(name);
			
			if (!actor.getExternalUrls().contains(currentUrl)) {
				actor.getExternalUrls().add(currentUrl);
			}
			
			getActorAttributes(seleniumId, actor);
			
			for (ActorAttribute attribute : actor.getAttributes()) {
				if (attribute.getKey().toLowerCase().equals("gender")) {
					actor.setGender(StringUtilitiesHigh.parseGender(attribute.getValue()));
					break;
				}
			}
			
			getActorImages(seleniumId, actor);
			
			if (!actor.getExternalUrls().contains(currentUrl)) {
				actor.addExternalUrl(currentUrl);
			}
			
			KinkPlugin.getActorDao().saveActor(actor);
			
			actor.unloadImages();
			
			return actor;
		}
		
		return actor;
	}
	
	public static void getActorAttributes(UUID seleniumId, Actor actor) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(KinkPlugin.getXpath("ActorAttributeCells"), SeleniumSelectorType.Xpath);
		int attributeCellCount = SeleniumServerUtilities.sendSeleniumCommand(command).getCount();
		
		String key;
		String value;
		for(int i = 1; i < attributeCellCount; i += 2) {
			command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
			command.setSelector(KinkPlugin.getXpath("ActorAttributeCells") + "[" + i + "]");
			key = SeleniumServerUtilities.sendSeleniumCommand(command).getText();

			command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadText);
			command.setSelector(KinkPlugin.getXpath("ActorAttributeCells") + "[" + (i + 1) + "]");
			value = SeleniumServerUtilities.sendSeleniumCommand(command).getText();
			
			key = WordUtils.capitalize(key);
			value = WordUtils.capitalize(value);

			if (key.endsWith(":")) {
				key = key.substring(0, key.length() - 1);
			}
			
			boolean updated = false;
			for (ActorAttribute attribute : actor.getAttributes()) {
				if (attribute.getKey().equals(key)) {
					attribute.setValue(value);
					updated = true;
					break;
				}
			}
			if (!updated) {
				actor.getAttributes().add(new ActorAttribute(key, value));
			}
			
		}
	}
	
	public static void getActorImages(UUID seleniumId, Actor actor) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Count);
		command.setSelector(KinkPlugin.getXpath("ActorImages"), SeleniumSelectorType.Xpath);
		int imageCount = SeleniumServerUtilities.sendSeleniumCommand(command).getCount();
		
		URL sourceUrl;
		KUtilsImage image;
		BufferedImage imageBuffer;
		
		String actorImageBase = KinkPlugin.getXpath("ActorImage");
		
		for (Integer i = 1; i <= imageCount; i++) {
			command = new SeleniumCommand(seleniumId, SeleniumCommandType.ReadAttribute);
			command.setSelector(MessageFormat.format(actorImageBase, i.toString()), SeleniumSelectorType.Xpath);
			command.setAttribute("src");
			
			try {
				sourceUrl = new URL(SeleniumServerUtilities.sendSeleniumCommand(command).getText());
				imageBuffer = ImageIO.read(sourceUrl);
				
				if (!ImageUtilitiesHigh.imageExists(imageBuffer, actor.getImages())) {
					image = new KUtilsImage(Paths.get("Actors", actor.getName(), "Images", (actor.getImages().size() + 1) + ".png").toString());
					
					image.setImage(imageBuffer);
					
					actor.getImages().add(image);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
