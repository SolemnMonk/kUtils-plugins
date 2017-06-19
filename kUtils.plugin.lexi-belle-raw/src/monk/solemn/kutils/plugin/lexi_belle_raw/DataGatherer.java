package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;

import hall.caleb.seltzer.enums.SelectorType;
import hall.caleb.seltzer.objects.response.MultiResultResponse;
import hall.caleb.seltzer.util.CommandFactory;
import hall.caleb.seltzer.util.SeltzerUtils;
import monk.solemn.kutils.objects.KUtilsImage;
import monk.solemn.kutils.objects.Rating;
import monk.solemn.kutils.objects.Shoot;
import monk.solemn.kutils.utilities.low.ImageUtilitiesLow;
import monk.solemn.kutils.utilities.low.StringUtilitiesLow;

public class DataGatherer {
	private static String cachedCoverUrl = null;
	
	public static void getShootDescription(UUID seleniumId, Shoot shoot) {
		String xpath = LexiBelleRawPlugin.getXpath("ShootDescription");
		MultiResultResponse response;
		response = (MultiResultResponse) SeltzerUtils.send(CommandFactory.newReadTextCommand(seleniumId, SelectorType.Xpath, xpath, 1));
		
		shoot.setDescription(response.getResults().get(0));
	}
	
	public static void cacheCoverImageUrl(UUID seleniumId, Integer index) {
		String xpath = LexiBelleRawPlugin.getXpath("VideoCoverImage");
		Integer column = (index % 2) + 1;
		index = (index + 1) / 2;
		xpath = MessageFormat.format(xpath, column, index);
		
		MultiResultResponse response;
		response = (MultiResultResponse) SeltzerUtils.send(CommandFactory.newReadAttributeCommand(seleniumId, SelectorType.Xpath, xpath, 1, "style"));
		
		String style = response.getResults().get(0);
		Pattern regex = Pattern.compile("background-image: *url\\((.*?)\\)");
		Matcher matcher = regex.matcher(style);
		matcher.find();
		String source = matcher.group(1);
		
		source = (source.startsWith("\"") ? source.substring(1) : source);
		source = (source.endsWith("\"") ? source.substring(0, source.length() - 1) : source);
		
		cachedCoverUrl = source;
	}
	
	public static void getShootCoverImage(UUID seleniumId, Shoot shoot) {
		KUtilsImage coverImage = shoot.getCoverImage();
		
		if (coverImage == null) {
			String name = StringUtilitiesLow.sanitizeForPathName(shoot.getTitle());
			coverImage = new KUtilsImage(Paths.get("Lexi Belle Raw", "Shoots", name, "poster.png").toString());
			shoot.setCoverImage(coverImage);
		}
		
		try {
			if (cachedCoverUrl == null) {
				new File(coverImage.getFilePath()).createNewFile();
				return;
			}
			
			String source = cachedCoverUrl;
			
			if (StringUtils.isNotBlank(source)) {
				URL sourceUrl = new URL(source);
				BufferedImage imageBuffer = ImageIO.read(sourceUrl);
				
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
		
		cachedCoverUrl = null;
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
