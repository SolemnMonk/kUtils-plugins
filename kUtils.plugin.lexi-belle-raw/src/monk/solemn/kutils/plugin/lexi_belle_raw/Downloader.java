package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;

import hall.caleb.selenium.enums.SelectorType;
import hall.caleb.selenium.objects.command.CommandFactory;
import hall.caleb.selenium.objects.command.MultiResultSelectorCommand;
import hall.caleb.selenium.objects.command.ReadAttributeCommand;
import hall.caleb.selenium.objects.response.MultiResultResponse;
import monk.solemn.kutils.enums.ShootType;
import monk.solemn.kutils.objects.QueuedTask;
import monk.solemn.kutils.objects.Shoot;
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;
import monk.solemn.kutils.utilities.high.ShootUtilities;
import monk.solemn.kutils.utilities.low.StringUtilitiesLow;

public class Downloader {
	public static Shoot downloadVideo(UUID seleniumId, QueuedTask task, String url) {
		SeleniumServerUtilities.sendSeleniumCommand(CommandFactory.newGoToCommand(seleniumId, url));
		
		MultiResultSelectorCommand command = CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath("ShootTitle"), 1);
		String title = ((MultiResultResponse) SeleniumServerUtilities.sendSeleniumCommand(command)).getResults().get(0);
		
		Shoot shoot;
		try {
			shoot = ShootUtilities.getShootByTitle(title);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		shoot.setShootType(ShootType.Video);
		
		DataGatherer.getShootDescription(seleniumId, shoot);
		DataGatherer.getShootRating(seleniumId, shoot);
		
		try {
			LexiBelleRawPlugin.getShootDao().saveShoot(shoot);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		String sanitizedTitle = StringUtilitiesLow.sanitizeForPathName(shoot.getTitle());
		
		downloadVideoFile(seleniumId, shoot, sanitizedTitle, task);
		
		return shoot;
	}
	
	private static void downloadVideoFile(UUID seleniumId, Shoot shoot, String sanitizedName, QueuedTask task) {
		ReadAttributeCommand command = CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, LexiBelleRawPlugin.getXpath("VideoSource"), 1, "src");
		MultiResultResponse response = (MultiResultResponse) SeleniumServerUtilities.sendSeleniumCommand(command);
		String src = response.getResults().get(0);
		
		Map<String, String> metadataMap = new HashMap<>();
		metadataMap.put("artist", "Lexi Belle");
		metadataMap.put("album_artist", "Lexi Belle Raw");
		metadataMap.put("composer", "Lexi Belle");
		metadataMap.put("POPM", ((Integer) ((Double) (shoot.getRating().getAvgRating() * 255)).intValue()).toString());
		metadataMap.put("TIT2", shoot.getDescription());		
		metadataMap.put("title", shoot.getTitle());
		
		String path = Paths.get("Videos", sanitizedName).toString();
		
		try {
			File movie = LexiBelleRawPlugin.getFileStorageDao().downloadFile(src, path, true);
			movie.renameTo(
					Paths.get(movie.getParent(), sanitizedName + "." + FilenameUtils.getExtension(movie.getName()))
							.toFile());
			movie = Paths.get(movie.getParent(), sanitizedName + "." + FilenameUtils.getExtension(movie.getName())).toFile();
			movie = LexiBelleRawPlugin.getFileStorageDao().applyMetadata(movie, metadataMap, null);
		} catch (IOException | SQLException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
