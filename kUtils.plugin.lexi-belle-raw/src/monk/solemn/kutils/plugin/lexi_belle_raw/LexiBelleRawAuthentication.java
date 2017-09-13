package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.sql.SQLException;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;

import monk.solemn.kutils.api.action.AuthenticateAction;
import monk.solemn.kutils.objects.Credentials;
import tech.seltzer.enums.CommandType;
import tech.seltzer.enums.SelectorType;
import tech.seltzer.objects.command.ChainCommandData;
import tech.seltzer.objects.command.CommandData;
import tech.seltzer.objects.command.GoToCommandData;
import tech.seltzer.objects.command.Selector;
import tech.seltzer.objects.command.selector.FillFieldCommandData;
import tech.seltzer.objects.command.selector.SelectorCommandData;
import tech.seltzer.objects.exception.SeltzerException;
import tech.seltzer.util.SeltzerSend;

@Component
public class LexiBelleRawAuthentication implements AuthenticateAction {
	@Override
	public boolean login() {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		Credentials credentials = null;
		try {
			credentials = LexiBelleRawPlugin.getCredentialDao().loadSiteCredentials("Lexi Belle Raw", null);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (credentials != null) {
			ChainCommandData<CommandData> command = new ChainCommandData<>(seltzerId);
			CommandData subCommand;
			
			subCommand = new GoToCommandData(seltzerId, LexiBelleRawPlugin.getUrl("Login"));
			command.getCommands().add(subCommand);

			String xpath = LexiBelleRawPlugin.getXpath("UsernameInput");
			String value = credentials.getUsername();
			subCommand = new FillFieldCommandData(seltzerId);
			((FillFieldCommandData) subCommand).setSelector(new Selector(SelectorType.XPATH, xpath));
			((FillFieldCommandData) subCommand).setText(value);
			command.getCommands().add(subCommand);
			
			xpath = LexiBelleRawPlugin.getXpath("PasswordInput");
			value = credentials.getPassword();
			subCommand = new FillFieldCommandData(seltzerId);
			((FillFieldCommandData) subCommand).setSelector(new Selector(SelectorType.XPATH, xpath));
			((FillFieldCommandData) subCommand).setText(value);
			command.getCommands().add(subCommand);
			
			xpath = LexiBelleRawPlugin.getXpath("LoginForm");
			subCommand = new SelectorCommandData(CommandType.FORM_SUBMIT, seltzerId);
			((FillFieldCommandData) subCommand).setSelector(new Selector(SelectorType.XPATH, xpath));
			command.getCommands().add(subCommand);
			
			command.serialize();
			try {
				SeltzerSend.send(command);
			} catch (SeltzerException e) {
				e.printStackTrace();
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void logout() {
		UUID seltzerId = LexiBelleRawPlugin.getSeltzerId();
		ChainCommandData<CommandData> command = new ChainCommandData<>(seltzerId);
		CommandData subCommand;

		String xpath = LexiBelleRawPlugin.getXpath("ProfileIcon");
		subCommand = new SelectorCommandData(CommandType.CLICK, seltzerId);
		((SelectorCommandData) subCommand).setSelector(new Selector(SelectorType.XPATH, xpath));
		command.getCommands().add(subCommand);
		
		xpath = LexiBelleRawPlugin.getXpath("LogoutLink");
		subCommand = new SelectorCommandData(CommandType.CLICK, seltzerId);
		((SelectorCommandData) subCommand).setSelector(new Selector(SelectorType.XPATH, xpath));
		command.getCommands().add(subCommand);
		
		command.serialize();
		try {
			SeltzerSend.send(command);
		} catch (SeltzerException e) {
			e.printStackTrace();
		}
	}
}
