package monk.solemn.kutils.plugin.lexi_belle_raw;

import java.sql.SQLException;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;

import hall.caleb.selenium.enums.SelectorType;
import hall.caleb.selenium.objects.command.ChainCommand;
import hall.caleb.selenium.objects.command.Command;
import hall.caleb.selenium.objects.command.CommandFactory;
import monk.solemn.kutils.api.authentication.SeleniumAuthentication;
import monk.solemn.kutils.objects.Credentials;
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;

@Component
public class LexiBelleRawAuthentication implements SeleniumAuthentication {
	@Override
	public boolean login(UUID seleniumId) {
		Credentials credentials = null;
		try {
			credentials = LexiBelleRawPlugin.getCredentialDao().loadSiteCredentials("Lexi Belle Raw", null);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (credentials != null) {
			ChainCommand command = new ChainCommand(seleniumId);
			Command subCommand;
			
			subCommand = CommandFactory.newGoToCommand(seleniumId, LexiBelleRawPlugin.getUrl("Login"));
			command.getCommands().add(subCommand);

			String xpath = LexiBelleRawPlugin.getXpath("UsernameInput");
			String value = credentials.getUsername();
			subCommand = CommandFactory.newFillFieldCommand(seleniumId, SelectorType.Xpath, xpath, value);
			command.getCommands().add(subCommand);
			
			xpath = LexiBelleRawPlugin.getXpath("PasswordInput");
			value = credentials.getPassword();
			subCommand = CommandFactory.newFillFieldCommand(seleniumId, SelectorType.Xpath, xpath, value);
			command.getCommands().add(subCommand);
			
			xpath = LexiBelleRawPlugin.getXpath("LoginForm");
			subCommand = CommandFactory.newFormSubmitCommand(seleniumId, SelectorType.Xpath, xpath);
			command.getCommands().add(subCommand);
			
			command.serialize();
			SeleniumServerUtilities.sendSeleniumCommand(command);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void logout(UUID seleniumId) {
		ChainCommand command = new ChainCommand(seleniumId);
		Command subCommand;

		String xpath = LexiBelleRawPlugin.getXpath("ProfileIcon");
		subCommand = CommandFactory.newClickCommand(seleniumId, SelectorType.Xpath, xpath);
		command.getCommands().add(subCommand);
		
		xpath = LexiBelleRawPlugin.getXpath("LogoutLink");
		subCommand = CommandFactory.newClickCommand(seleniumId, SelectorType.Xpath, xpath);
		command.getCommands().add(subCommand);
		
		command.serialize();
		SeleniumServerUtilities.sendSeleniumCommand(command);
	}
}
