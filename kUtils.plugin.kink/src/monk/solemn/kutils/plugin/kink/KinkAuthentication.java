package monk.solemn.kutils.plugin.kink;

import java.sql.SQLException;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;

import hall.caleb.selenium.enums.SeleniumCommandType;
import hall.caleb.selenium.enums.SeleniumSelectorType;
import hall.caleb.selenium.objects.SeleniumCommand;
import monk.solemn.kutils.api.authentication.SeleniumAuthentication;
import monk.solemn.kutils.objects.Credentials;
import monk.solemn.kutils.utilities.high.SeleniumServerUtilities;

@Component
public class KinkAuthentication implements SeleniumAuthentication {
	@Override
	public boolean login(UUID seleniumId) {
		Credentials credentials = null;
		try {
			credentials = KinkPlugin.getCredentialDao().loadNetworkCredentials("Kink.com", null);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (credentials != null) {
			SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.Chain);
			SeleniumCommand subCommand;
			
			subCommand = new SeleniumCommand(seleniumId, SeleniumCommandType.GoTo);
			subCommand.setUrl(KinkPlugin.getUrl("Login"));
			command.getCommands().add(subCommand);

			subCommand = new SeleniumCommand(seleniumId, SeleniumCommandType.Click);
			subCommand.setSelector(KinkPlugin.getXpath("ContentPreferencesDismiss"), SeleniumSelectorType.Xpath);
			command.getCommands().add(subCommand);
			
			subCommand = new SeleniumCommand(seleniumId, SeleniumCommandType.FillField);
			subCommand.setSelector(KinkPlugin.getXpath("LoginUsername"), SeleniumSelectorType.Xpath);
			subCommand.setText(credentials.getUsername());
			command.getCommands().add(subCommand);
			
			subCommand = new SeleniumCommand(seleniumId, SeleniumCommandType.FillField);
			subCommand.setSelector(KinkPlugin.getXpath("LoginPassword"), SeleniumSelectorType.Xpath);
			subCommand.setText(credentials.getPassword());
			command.getCommands().add(subCommand);
			
			subCommand = new SeleniumCommand(seleniumId, SeleniumCommandType.FormSubmit);
			subCommand.setSelector(KinkPlugin.getXpath("LoginForm"), SeleniumSelectorType.Xpath);
			command.getCommands().add(subCommand);
			
			SeleniumServerUtilities.sendSeleniumCommand(command);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void logout(UUID seleniumId) {
		SeleniumCommand command = new SeleniumCommand(seleniumId, SeleniumCommandType.GoTo);
		command.setUrl(KinkPlugin.getUrl("Logout"));
		
		SeleniumServerUtilities.sendSeleniumCommand(command);
	}
}
