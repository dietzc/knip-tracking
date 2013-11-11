/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package org.knime.knip.tracking.trackmate;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class LAPTrackletCreatorNodePlugin extends Plugin {
    // The shared instance.
    private static LAPTrackletCreatorNodePlugin plugin;

    /**
     * The constructor.
     */
    public LAPTrackletCreatorNodePlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

    }

    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     * 
     * @return Singleton instance of the Plugin
     */
    public static LAPTrackletCreatorNodePlugin getDefault() {
        return plugin;
    }

}

