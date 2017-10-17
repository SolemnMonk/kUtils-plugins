package monk.solemn.kutils.plugin.lexi_belle_raw.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import monk.solemn.kutils.api.action.GatherDataAction;
import monk.solemn.kutils.api.action.MetadataType;
import monk.solemn.kutils.plugin.lexi_belle_raw.LbrPlugin;
import tech.seltzer.enums.CommandType;
import tech.seltzer.enums.SelectorType;
import tech.seltzer.objects.command.CommandData;
import tech.seltzer.objects.command.Selector;
import tech.seltzer.objects.command.selector.multiresult.MultiResultSelectorCommandData;
import tech.seltzer.objects.exception.SeltzerException;
import tech.seltzer.objects.response.MultiResultResponse;
import tech.seltzer.objects.response.SingleResultResponse;
import tech.seltzer.util.SeltzerSend;

public class LbrGatherDataAction implements GatherDataAction {

	@Override
	public boolean requiresAuthentication() {
		return true;
	}

	@Override
	public String getTitle() {
		UUID seltzerId = LbrPlugin.getSeltzerId();
		
		MultiResultSelectorCommandData cmd = new MultiResultSelectorCommandData(CommandType.READ_TEXT, seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, LbrPlugin.getXpath("BundleTitle")));
		cmd.setMaxResults(1);
		
		try {
			return ((MultiResultResponse) SeltzerSend.send(cmd)).getResults().get(0);
		} catch (SeltzerException e) {
			e.printStackTrace();
			return "";
		}
	}

	@Override
	public String getDescription() {
		UUID seltzerId = LbrPlugin.getSeltzerId();
		
		MultiResultSelectorCommandData cmd = new MultiResultSelectorCommandData(CommandType.READ_TEXT, seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, LbrPlugin.getXpath("BundleDescription")));
		cmd.setMaxResults(1);
		
		try {
			return ((MultiResultResponse) SeltzerSend.send(cmd)).getResults().get(0);
		} catch (SeltzerException e) {
			e.printStackTrace();
			return "";
		}
	}

	@Override
	public String getMetadata(MetadataType metadataType) {
		switch(metadataType) {
		case RATING:
			return getRating().get(MetadataType.RATING);
		case RATING_COUNT:
			return getRating().get(MetadataType.RATING_COUNT);
		case TITLE:
			return getTitle();
		case DESCRIPTION:
			return getDescription();
		case URL:
			return getURL();
		case ACTORS:
			return getActors();
		case COVER_IMAGE:
		case BANNER_IMAGE:
			return getCoverImage();
		default:
			return "";
		}
	}

	@Override
	public Map<MetadataType, String> getAllMetadata() {
		Map<MetadataType, String> metadata = new HashMap<>();
		
		metadata.putAll(getRating());
		metadata.put(MetadataType.COVER_IMAGE, getCoverImage());
		metadata.put(MetadataType.BANNER_IMAGE, getCoverImage());
		metadata.put(MetadataType.TITLE, getTitle());
		metadata.put(MetadataType.DESCRIPTION, getDescription());
		metadata.put(MetadataType.ACTORS, getActors());
		metadata.put(MetadataType.URL, getURL());
		
		return metadata;
	}

	private String getCoverImage() {
		return null;
	}

	private Map<MetadataType, String> getRating() {
		Map<MetadataType, String> ratingData = new HashMap<>();
		
		UUID seltzerId = LbrPlugin.getSeltzerId();
		
		MultiResultSelectorCommandData cmd = new MultiResultSelectorCommandData(CommandType.READ_TEXT, seltzerId);
		cmd.setMaxResults(1);
		
		try {
			cmd.setSelector(new Selector(SelectorType.XPATH, LbrPlugin.getXpath("ThumbsDown")));
			Integer thumbsDown = Integer.parseInt(((MultiResultResponse) SeltzerSend.send(cmd)).getResults().get(0));
			
			cmd.setSelector(new Selector(SelectorType.XPATH, LbrPlugin.getXpath("Thumbs")));
			Integer thumbsUp = Integer.parseInt(((MultiResultResponse) SeltzerSend.send(cmd)).getResults().get(0));
			
			ratingData.put(MetadataType.RATING_COUNT, String.valueOf(thumbsUp + thumbsDown));
			ratingData.put(MetadataType.RATING, String.valueOf(thumbsUp / (thumbsUp + thumbsDown)));
		} catch (SeltzerException e) {
			e.printStackTrace();
		}
		
		return ratingData;
	}

	private String getActors() {
		List<String> actors = new ArrayList<>();
		
		actors.add("Lexi Belle");
		
		UUID seltzerId = LbrPlugin.getSeltzerId();
		
		MultiResultSelectorCommandData cmd = new MultiResultSelectorCommandData(CommandType.READ_TEXT, seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, LbrPlugin.getXpath("ActorList")));
		
		try {
			MultiResultResponse response = (MultiResultResponse) SeltzerSend.send(cmd);
			for (String actor : response.getResults()) {
				actors.add(actor);
			}
		} catch (SeltzerException e) {
			e.printStackTrace();
		}
		
		return String.join("/", actors);
	}

	private String getURL() {
		UUID seltzerId = LbrPlugin.getSeltzerId();
		
		CommandData cmd = new CommandData(CommandType.GET_URL, seltzerId);
		
		try {
			return ((SingleResultResponse) SeltzerSend.send(cmd)).getResult();
		} catch (SeltzerException e) {
			e.printStackTrace();
			return "";
		}
	}

}
