package monk.solemn.kutils.plugin.lexi_belle_raw;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	private static BundleContext context;
	
	@Override
	public void start(BundleContext context) throws Exception {
		Activator.context = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
	
	public static BundleContext getContext() {
		return context;
	}
}
