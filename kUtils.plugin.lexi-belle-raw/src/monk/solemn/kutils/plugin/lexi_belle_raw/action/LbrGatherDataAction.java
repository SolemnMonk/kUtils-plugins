package monk.solemn.kutils.plugin.lexi_belle_raw.action;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import monk.solemn.kutils.api.action.GatherDataAction;
import monk.solemn.kutils.api.action.MetadataType;
import monk.solemn.kutils.plugin.lexi_belle_raw.LexiBelleRawPlugin;
import tech.seltzer.enums.CommandType;
import tech.seltzer.enums.SelectorType;
import tech.seltzer.objects.command.Selector;
import tech.seltzer.objects.command.selector.multiresult.MultiResultSelectorCommandData;
import tech.seltzer.objects.exception.SeltzerException;
import tech.seltzer.objects.response.MultiResultResponse;
import tech.seltzer.util.SeltzerSend;

public class LbrGatherDataAction implements GatherDataAction {

	@Override
	public boolean requiresAuthentication() {
		return true;
	}

	@Override
	public String getTitle() {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		MultiResultSelectorCommandData cmd = new MultiResultSelectorCommandData(CommandType.READ_TEXT, seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("BundleTitle")));
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
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		MultiResultSelectorCommandData cmd = new MultiResultSelectorCommandData(CommandType.READ_TEXT, seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("BundleDescription")));
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
	}

	@Override
	public Map<MetadataType, String> getAllMetadata() {
		Map<MetadataType, String> metadata = new HashMap<>();
		
		metadata.put(MetadataType.RATING, getRating());
		metadata.put(MetadataType.RATING_COUNT, getRatingCount());
		metadata.put(MetadataType.COVER_IMAGE, getCoverImage());
		metadata.put(MetadataType.BANNER_IMAGE, getCoverImage());
		metadata.put(MetadataType.TITLE, getTitle());
		metadata.put(MetadataType.DESCRIPTION, getDescription());
		metadata.putAll(getActors());
		metadata.put(MetadataType.URL, getURL());
		
		return metadata;
	}

}
