package monk.solemn.kutils.plugin.lexi_belle_raw.action;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;

import monk.solemn.kutils.api.action.ListAction;
import monk.solemn.kutils.plugin.lexi_belle_raw.LexiBelleRawPlugin;
import tech.seltzer.enums.CommandType;
import tech.seltzer.enums.ResponseType;
import tech.seltzer.enums.SelectorType;
import tech.seltzer.enums.SeltzerKeys;
import tech.seltzer.objects.command.ChainCommandData;
import tech.seltzer.objects.command.CommandData;
import tech.seltzer.objects.command.Selector;
import tech.seltzer.objects.command.selector.SelectorCommandData;
import tech.seltzer.objects.command.selector.SendKeyCommandData;
import tech.seltzer.objects.command.selector.multiresult.MultiResultSelectorCommandData;
import tech.seltzer.objects.command.selector.multiresult.ReadAttributeCommandData;
import tech.seltzer.objects.exception.SeltzerException;
import tech.seltzer.objects.response.ChainResponse;
import tech.seltzer.objects.response.MultiResultResponse;
import tech.seltzer.objects.response.Response;
import tech.seltzer.objects.response.SingleResultResponse;
import tech.seltzer.util.SeltzerSend;

public class LbrListAction implements ListAction {

	@Override
	public boolean requiresAuthentication() {
		return true;
	}

	@Override
	public long numChannels() {
		return 2;
	}
	
	@Override
	public long numItems() {
		try {
			if (!isChannelPage()) {
				return 0;
			} else {
				return countItems();
			}
		} catch (SeltzerException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	@Override
	public long numBundles() {
		try {
			if (!isItemPage()) {
				return 0;
			} else {
				return countBundles();
			}
		} catch (SeltzerException e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public List<String> listChannels() {
		List<String> channels = new ArrayList<>();
		channels.add(LexiBelleRawPlugin.getUrl("VideoChannel"));
		channels.add(LexiBelleRawPlugin.getUrl("PhotosetChannel"));
		return channels;
	}

	@Override
	public List<String> listItems() {
		try {
			if (!isChannelPage()) {
				return new ArrayList<>();
			} else {
				return getItems();
			}
		} catch (SeltzerException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<String> listBundles() {
		try {
			if (!isItemPage()) {
				return new ArrayList<>();
			} else {
				return getBundles();
			}
		} catch (SeltzerException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean isPaginated() {
		return false;
	}

	@Override
	public int getNumPages() {
		return 1;
	}
	
	@Override
	public int getCurrentPage() {
		return 0;
	}
	
	@Override
	public boolean hasPreviousPage() {
		return false;
	}

	@Override
	public boolean hasNextPage() {
		return false;
	}

	@Override
	public void previousPage() {
		return;
	}

	@Override
	public void nextPage() {
		return;
	}

	private boolean isChannelPage() throws SeltzerException {
		String xpath = LexiBelleRawPlugin.getXpath("PageTitle");
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		MultiResultSelectorCommandData cmd = new MultiResultSelectorCommandData(CommandType.READ_TEXT, seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, xpath));
		cmd.setMaxResults(1);
		
		MultiResultResponse resp = (MultiResultResponse) SeltzerSend.send(cmd);
		List<String> results = resp.getResults();
		
		if (CollectionUtils.isNotEmpty(results)) {
			results.set(0, results.get(0).toLowerCase());
			return (results.get(0).equals("videos") || results.get(0).equals("photosets"));
		} else {
			return false;
		}
	}
	
	private boolean isItemPage() throws SeltzerException {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		CommandData cmd = new CommandData(CommandType.GET_URL, seltzerId);
		SingleResultResponse resp = (SingleResultResponse) SeltzerSend.send(cmd);
		
		String url = resp.getResult();
		
		Pattern pattern = Pattern.compile(LexiBelleRawPlugin.getRegex("ItemUrl"));
		Matcher matcher = pattern.matcher(url);
		
		return matcher.matches();
	}

	private long countItems() throws SeltzerException {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		SelectorCommandData cmd = new SelectorCommandData(CommandType.COUNT, seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("ItemGrid")));
		
		return Long.parseLong(((SingleResultResponse) SeltzerSend.send(cmd)).getResult());
	}
	
	private long countBundles() throws SeltzerException {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		SelectorCommandData cmd = new SelectorCommandData(CommandType.COUNT, seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("ItemGrid")));
		
		return Long.parseLong(((SingleResultResponse) SeltzerSend.send(cmd)).getResult());
	}
	
	private List<String> getItems() throws SeltzerException {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		ReadAttributeCommandData cmd = new ReadAttributeCommandData(seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("ItemGrid")));
		cmd.setAttribute("href");
		cmd.setMaxResults(0);
		
		return ((MultiResultResponse) SeltzerSend.send(cmd)).getResults();
	}
	
	private List<String> getBundles() throws SeltzerException {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		CommandData urlCmd = new CommandData(CommandType.GET_URL, seltzerId);
		SingleResultResponse resp = (SingleResultResponse) SeltzerSend.send(urlCmd);
		
		String url = resp.getResult();
		
		Pattern pattern = Pattern.compile(LexiBelleRawPlugin.getRegex("ItemUrl"));
		Matcher matcher = pattern.matcher(url);
		
		if (matcher.matches()) {
			if (matcher.group(1).equals("scene")) {
				return getVideoBundle();
			} else if (matcher.group(1).equals("gallery")) {
				return getPhotosetBundles();
			} else {
				return new ArrayList<>();
			}
		} else {
			return new ArrayList<>();
		}
	}

	private List<String> getVideoBundle() throws SeltzerException {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		ReadAttributeCommandData cmd = new ReadAttributeCommandData(seltzerId);
		cmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("VideoSource")));
		cmd.setAttribute("src");
		cmd.setMaxResults(1);
		
		return ((MultiResultResponse) SeltzerSend.send(cmd)).getResults();
	}

	private List<String> getPhotosetBundles() throws SeltzerException {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		
		SelectorCommandData countCmd = new SelectorCommandData(CommandType.COUNT, seltzerId);
		countCmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("ShowAllPhotosButton")));
		
		SelectorCommandData clickCmd;
		
		if (Integer.parseInt(((SingleResultResponse) SeltzerSend.send(countCmd)).getResult()) == 1) {
			clickCmd = new SelectorCommandData(CommandType.COUNT, seltzerId);
			clickCmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("ShowAllPhotosButton")));
			
			SeltzerSend.send(clickCmd);
		}
		
		clickCmd = new SelectorCommandData(CommandType.COUNT, seltzerId);
		clickCmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("FirstImage")));
		
		SeltzerSend.send(clickCmd);
		
		MultiResultSelectorCommandData readCmd = new MultiResultSelectorCommandData(CommandType.READ_TEXT, seltzerId);
		readCmd.setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("ImageCountLabel")));
		readCmd.setMaxResults(1);
		
		MultiResultResponse readResp = (MultiResultResponse) SeltzerSend.send(readCmd);
		Long imageCount = Long.parseLong(readResp.getResults().get(0));
		
		ChainCommandData<CommandData> chain = new ChainCommandData<>(seltzerId);
		CommandData subCmd;
		
		for (long i = 0; i < imageCount; i++) {
			subCmd = new ReadAttributeCommandData(seltzerId);
			((ReadAttributeCommandData) subCmd).setSelector(new Selector(SelectorType.XPATH, LexiBelleRawPlugin.getXpath("FullscreenImage")));
			((ReadAttributeCommandData) subCmd).setAttribute("src");
			((ReadAttributeCommandData) subCmd).setMaxResults(1);
			chain.addCommand(subCmd);
			
			subCmd = new SendKeyCommandData(seltzerId);
			((SendKeyCommandData) subCmd).setKey(SeltzerKeys.ARROW_RIGHT);
			chain.addCommand(subCmd);
		}
		
		@SuppressWarnings("unchecked")
		ChainResponse<Response> chainResp = (ChainResponse<Response>) SeltzerSend.send(readCmd);
		
		List<String> urls = new ArrayList<>();
		
		for (Response r : chainResp.getResponses()) {
			if (r.getType() == ResponseType.SINGLE_RESULT) {
				urls.add(((SingleResultResponse) r).getResult());
			}
		}
		
		return urls;
	}
}
