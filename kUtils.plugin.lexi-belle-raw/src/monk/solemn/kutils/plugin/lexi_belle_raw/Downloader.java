package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import hall.caleb.seltzer.enums.SelectorType;
import hall.caleb.seltzer.objects.command.ChainCommand;
import hall.caleb.seltzer.objects.command.Command;
import hall.caleb.seltzer.objects.command.MultiResultSelectorCommand;
import hall.caleb.seltzer.objects.command.ReadAttributeCommand;
import hall.caleb.seltzer.objects.command.WaitCommand;
import hall.caleb.seltzer.objects.response.MultiResultResponse;
import hall.caleb.seltzer.objects.response.Response;
import hall.caleb.seltzer.objects.response.SingleResultResponse;
import hall.caleb.seltzer.util.CommandFactory;
import hall.caleb.seltzer.util.SeltzerUtils;
import monk.solemn.kutils.enums.ShootType;
import monk.solemn.kutils.objects.KUtilsImage;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.objects.Shoot;
import monk.solemn.kutils.utilities.high.ImageUtilitiesHigh;
import monk.solemn.kutils.utilities.high.ShootUtilities;
import monk.solemn.kutils.utilities.low.StringUtilitiesLow;

public class Downloader {
	public static Shoot downloadVideo(UUID seleniumId, QueuedTask task, String url) {
		SeltzerUtils.send(CommandFactory.newGoToCommand(seleniumId, url));
		
		waitForPage(seleniumId, new String[] {"VideoShootTitle", "ThumbsUp", "ThumbsDown", "VideoPlayer"});
		
		MultiResultSelectorCommand command = CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath("VideoShootTitle"), 1);
		String title = ((MultiResultResponse) SeltzerUtils.send(command)).getResults().get(0);
		
		Shoot shoot;
		try {
			shoot = ShootUtilities.getShootByTitle(title);
			shoot.setExternalUrl(((SingleResultResponse) SeltzerUtils.send(CommandFactory.newGetUrlCommand(seleniumId))).getResult());
			shoot.setSite("Lexi Belle Raw");
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		shoot.setShootType(ShootType.Video);
		
		DataGatherer.getShootDescription(seleniumId, shoot);
		DataGatherer.getShootRating(seleniumId, shoot);
		DataGatherer.getShootActors(seleniumId, shoot);
		DataGatherer.getShootCoverImage(seleniumId, shoot);
		
		try {
			LexiBelleRawPlugin.getShootDao().saveShoot(shoot);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		String sanitizedTitle = StringUtilitiesLow.sanitizeForPathName(shoot.getTitle());
		System.out.println(sanitizedTitle);
		downloadVideoFile(seleniumId, shoot, sanitizedTitle, task);
		
		return shoot;
	}
	
	public static Shoot downloadImageSet(UUID seleniumId, QueuedTask task, String url) {
		SeltzerUtils.send(CommandFactory.newGoToCommand(seleniumId, url));
		
		waitForPage(seleniumId, new String[] {"ImageShootTitle", "FirstImage", "ThumbsUp", "ThumbsDown"});
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		MultiResultSelectorCommand command = CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath("ImageShootTitle"), 1);
		String title = ((MultiResultResponse) SeltzerUtils.send(command)).getResults().get(0);
		
		Shoot shoot;
		try {
			shoot = ShootUtilities.getShootByTitle(title);
			shoot.setExternalUrl(((SingleResultResponse) SeltzerUtils.send(CommandFactory.newGetUrlCommand(seleniumId))).getResult());
			shoot.setSite("Lexi Belle Raw");
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		shoot.setShootType(ShootType.Images);
		
		DataGatherer.getShootDescription(seleniumId, shoot);
		DataGatherer.getShootRating(seleniumId, shoot);
		DataGatherer.getShootActors(seleniumId, shoot);
		DataGatherer.getShootCoverImage(seleniumId, shoot);
		
		try {
			LexiBelleRawPlugin.getShootDao().saveShoot(shoot);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		String sanitizedTitle = StringUtilitiesLow.sanitizeForPathName(shoot.getTitle());
		System.out.println(sanitizedTitle);
		downloadImageSet(seleniumId, shoot, sanitizedTitle, task);
		
		return shoot;
	}
	
	private static void downloadVideoFile(UUID seleniumId, Shoot shoot, String sanitizedName, QueuedTask task) {
		ReadAttributeCommand command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath("VideoSource"), 1, "src");
		MultiResultResponse response = (MultiResultResponse) SeltzerUtils.send(command);
		String src = response.getResults().get(0);
		
		String auth = ((SingleResultResponse) SeltzerUtils.send(CommandFactory.newGetCookieCommand(seleniumId, "user_auth"))).getResult();
		
		Map<String, String> cookieMap = new HashMap<>();
		cookieMap.put("user_auth", auth);
		
		Map<String, String> metadataMap = new HashMap<>();
		metadataMap.put("artist", "Lexi Belle");
		metadataMap.put("album_artist", "Lexi Belle Raw");
		metadataMap.put("composer", "Lexi Belle");
		metadataMap.put("POPM", ((Integer) ((Double) (shoot.getRating().getAvgRating() * 255)).intValue()).toString());
		metadataMap.put("TIT2", shoot.getDescription());		
		metadataMap.put("title", shoot.getTitle());
		
		String path = Paths.get("Lexi Belle Raw", sanitizedName).toString();
		
		try {
			File movie = LexiBelleRawPlugin.getFileStorageDao().downloadFile(src, path, true, seleniumId, cookieMap);
			movie.renameTo(
					Paths.get(movie.getParent(), sanitizedName + "." + FilenameUtils.getExtension(movie.getName()))
							.toFile());
			movie = Paths.get(movie.getParent(), sanitizedName + "." + FilenameUtils.getExtension(movie.getName())).toFile();
			movie = LexiBelleRawPlugin.getFileStorageDao().applyMetadata(movie, metadataMap, null);
		} catch (IOException | SQLException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void downloadImageSet(UUID seleniumId, Shoot shoot, String sanitizedName, QueuedTask task) {
		String firstImageXpath = LexiBelleRawPlugin.getXpath("FirstImage");
		String sidebarToggleXpath = LexiBelleRawPlugin.getXpath("GallerySidebarToggle");
		String fullImageXpath = LexiBelleRawPlugin.getXpath("FullscreenImage");
		String nextImageXpath = LexiBelleRawPlugin.getXpath("NextImageButton");
		String imageCountXpath = LexiBelleRawPlugin.getXpath("ImageCountLabel");
		
		Command command;
		Response response;
		
		Integer imageCount; 
		Integer fileNameLength;
		
		String imageSource;
		String paddedName;
		URL imageSourceUrl;
		BufferedImage imageBuffer;
		KUtilsImage image;
		
		command = CommandFactory.newWaitCommand(seleniumId, SelectorType.Xpath, firstImageXpath, 30);
		SeltzerUtils.send(command);
		
		command = CommandFactory.newClickCommand(seleniumId, SelectorType.Xpath, firstImageXpath);
		SeltzerUtils.send(command);
		
		command = CommandFactory.newWaitCommand(seleniumId, SelectorType.Xpath, sidebarToggleXpath, 30);
		SeltzerUtils.send(command);
		
		command = CommandFactory.newClickCommand(seleniumId, SelectorType.Xpath, sidebarToggleXpath);
		SeltzerUtils.send(command);
		
		command = CommandFactory.newWaitCommand(seleniumId, SelectorType.Xpath, imageCountXpath, 30);
		SeltzerUtils.send(command);
		
		command = CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, imageCountXpath, 1);
		response = SeltzerUtils.send(command);
		imageCount = Integer.parseInt(((MultiResultResponse) response).getResults().get(0));
		fileNameLength = imageCount.toString().length() + 1;
		
		List<KUtilsImage> previewImages = new LinkedList<KUtilsImage>();
		previewImages.addAll(shoot.getPreviewImages());
		
		for (Integer i = 1; i <= imageCount; i++) {
			command = CommandFactory.newWaitCommand(seleniumId, SelectorType.Xpath, fullImageXpath, 30);
			SeltzerUtils.send(command);
			
			command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, fullImageXpath, 1, "src");
			response = SeltzerUtils.send(command);
			imageSource = ((MultiResultResponse) response).getResults().get(0);
			
			try {
				if (StringUtils.isNotBlank(imageSource)) {
					imageSourceUrl = new URL(imageSource);
					imageBuffer = ImageIO.read(imageSourceUrl);
					
					if (!ImageUtilitiesHigh.imageExists(imageBuffer, shoot.getPreviewImages())) {
						paddedName = i.toString();
						while (paddedName.length() < fileNameLength) {
							paddedName = "0" + paddedName;
						}
						
						image = new KUtilsImage(Paths.get("Lexi Belle Raw", sanitizedName, paddedName +  ".png").toString());
						image.setImage(imageBuffer);
						previewImages.add(image);
					}
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			command = CommandFactory.newClickCommand(seleniumId, SelectorType.Xpath, nextImageXpath);
			SeltzerUtils.send(command);
		}
		
		shoot.setPreviewImages(previewImages);
	}
	
	private static void waitForPage(UUID seleniumId, String[] keys) {
		ChainCommand chain = new ChainCommand(seleniumId);
		WaitCommand wait;
		for (String key : keys) {
			wait = CommandFactory.newWaitCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath(key), 30);
			chain.getCommands().add(wait);
		}
		SeltzerUtils.send(chain);
	}
}
