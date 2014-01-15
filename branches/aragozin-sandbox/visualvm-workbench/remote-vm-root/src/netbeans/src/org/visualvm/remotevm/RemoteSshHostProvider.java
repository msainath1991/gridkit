/**
 * Copyright 2012-2014 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.visualvm.remotevm;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FilenameFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceContainer;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemoteSshHostProvider {

	private static final Logger LOGGER = Logger.getLogger(RemoteSshHostProvider.class.getName());

	private static final String SNAPSHOT_VERSION = "snapshot_version"; // NOI18N
	private static final String SNAPSHOT_VERSION_DIVIDER = ".";
	private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1"; // NOI18N
	private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0"; // NOI18N
	private static final String CURRENT_SNAPSHOT_VERSION = 
			CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;

	private static final String PROPERTY_USER = "prop_user"; // NOI18N
	private static final String PROPERTY_HOSTNAME = "prop_hostname"; // NOI18N
	private static final String PROPERTY_JAVA_CMD = "prop_java_cmd"; // NOI18N

	private static final String DEFAULT_USER = System.getProperty("user.name");

	private Semaphore hostsLockedSemaphore = new Semaphore(1);

	@SuppressWarnings("unchecked")
	public RemoteSshHost createHost(final RemoteSshHostProperties hostDescriptor, final boolean createOnly, final boolean interactive) {
		try {

			lockHosts();

			final String hostName = hostDescriptor.getHostName();
			InetAddress inetAddress = null;
			ProgressHandle pHandle = null;

			try {
				pHandle = ProgressHandleFactory.createHandle(NbBundle
						.getMessage(RemoteSshHostProvider.class,
								"LBL_Searching_for_host")
						+ hostName); // NOI18N
				pHandle.setInitialDelay(0);
				pHandle.start();
				try {
					inetAddress = InetAddress.getByName(hostName);
				} catch (UnknownHostException e) {
					if (interactive) {
						DialogDisplayer.getDefault().notifyLater(
								new NotifyDescriptor.Message(NbBundle
										.getMessage(RemoteSshHostProvider.class,
												"MSG_Wrong_Host", hostName),
										NotifyDescriptor. // NOI18N
										ERROR_MESSAGE));
					}
				}
			} finally {
				final ProgressHandle pHandleF = pHandle;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (pHandleF != null)
							pHandleF.finish();
					}
				});
			}

			if (inetAddress != null) {
				final RemoteSshHost knownHost = getHostByAddressImpl(inetAddress);
				if (knownHost != null) {
					if (interactive && createOnly) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								ExplorerSupport.sharedInstance()
										.selectDataSource(knownHost);
								DialogDisplayer
										.getDefault()
										.notifyLater(
												new NotifyDescriptor.Message(
														NbBundle.getMessage(
																RemoteSshHostProvider.class,
																"MSG_Already_Monitored",
																new Object[] // NOI18N
																{
																		hostName,
																		DataSourceDescriptorFactory
																				.getDescriptor(
																						knownHost)
																				.getName() }),
														NotifyDescriptor.WARNING_MESSAGE));
							}
						});
					}
					return knownHost;
				} else {
					String ipString = inetAddress.getHostAddress();

					String[] propNames = new String[] { 
							SNAPSHOT_VERSION,
							PROPERTY_HOSTNAME,
							DataSourceDescriptor.PROPERTY_NAME };
					
					String[] propValues = new String[] {
							CURRENT_SNAPSHOT_VERSION, hostName,
							hostDescriptor.getDisplayName() };

					File customPropertiesStorage = Utils.getUniqueFile(
							RemoteSshHostsSupport.getStorageDirectory(), 
							ipString,
							Storage.DEFAULT_PROPERTIES_EXT);
					
					Storage storage = new Storage(
							customPropertiesStorage.getParentFile(),
							customPropertiesStorage.getName());
					
					storage.setCustomProperties(propNames, propValues);

					RemoteSshHost newHost = null;
					try {
						newHost = new RemoteSshHost(DEFAULT_USER, hostName, storage);
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "Error creating host", e); // Should never happen // NOI18N
					}

					if (newHost != null) {
						DataSourceContainer remoteHosts = RemoteSshHostsContainer
								.sharedInstance().getRepository();
						Set<RemoteSshHost> remoteHostsSet = remoteHosts
								.getDataSources(RemoteSshHost.class);
						if (!createOnly && remoteHostsSet.contains(newHost)) {
							storage.deleteCustomPropertiesStorage();
							Iterator<RemoteSshHost> existingHosts = remoteHostsSet
									.iterator();
							while (existingHosts.hasNext()) {
								RemoteSshHost existingHost = existingHosts.next();
								if (existingHost.equals(newHost)) {
									newHost = existingHost;
									break;
								}
							}
						} else {
							if (hostDescriptor.getPropertiesCustomizer() != null)
								hostDescriptor.getPropertiesCustomizer()
										.propertiesDefined(newHost);
							remoteHosts.addDataSource(newHost);
						}
					}
					
					RemoteApplication app = new RemoteApplication(newHost, "100");
					newHost.getRepository().addDataSource(app);
					
					return newHost;
				}
			}
			return null;

		} catch (InterruptedException ex) {
			LOGGER.throwing(RemoteSshHostProvider.class.getName(), "createHost", ex); // NOI18N
			return null;
		} finally {
			unlockHosts();
		}
	}

	void removeHost(RemoteSshHost host, boolean interactive) {
		try {
			lockHosts();

			// TODO: if interactive, show a Do-Not-Show-Again confirmation
			// dialog
			DataSource owner = host.getOwner();
			if (owner != null) {
				owner.getRepository().removeDataSource(host);
			}

		} catch (InterruptedException ex) {
			LOGGER.throwing(RemoteSshHostProvider.class.getName(), "removeHost", ex); // NOI18N
		} finally {
			unlockHosts();
		}
	}

	private RemoteSshHost getHostByAddressImpl(InetAddress inetAddress) {
		Set<RemoteSshHost> knownHosts = DataSourceRepository.sharedInstance()
				.getDataSources(RemoteSshHost.class);
		for (RemoteSshHost knownHost : knownHosts)
			if (knownHost.getInetAddress().equals(inetAddress))
				return knownHost;

		return null;
	}

	private void initPersistedHosts() {
		if (RemoteSshHostsSupport.storageDirectoryExists()) {
			File storageDir = RemoteSshHostsSupport.getStorageDirectory();
			File[] files = storageDir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(Storage.DEFAULT_PROPERTIES_EXT);
				}
			});

			Set<File> unresolvedHostsF = new HashSet<File>();
			Set<String> unresolvedHostsS = new HashSet<String>();

			Set<RemoteSshHost> hosts = new HashSet<RemoteSshHost>();
			for (File file : files) {

				Storage storage = new Storage(storageDir, file.getName());
				String hostName = storage.getCustomProperty(PROPERTY_HOSTNAME);

				RemoteSshHost persistedHost = null;
				try {
					persistedHost = new RemoteSshHost(DEFAULT_USER, hostName, storage);
				} catch (Exception e) {
					LOGGER.throwing(RemoteSshHostProvider.class.getName(),
							"initPersistedHosts", e); // NOI18N
					unresolvedHostsF.add(file);
					unresolvedHostsS.add(hostName);
				}

				if (persistedHost != null) {
					hosts.add(persistedHost);
				}
			}

			if (!unresolvedHostsF.isEmpty()) {
				notifyUnresolvedHosts(unresolvedHostsF, unresolvedHostsS);
			}

			System.err.println("Adding sources: " + hosts);
			
			RemoteSshHostsContainer.sharedInstance().getRepository()
					.addDataSources(hosts);
		}
	}

	private static void notifyUnresolvedHosts(final Set<File> unresolvedHostsF,
			final Set<String> unresolvedHostsS) {
		RequestProcessor.getDefault().post(new Runnable() {
			public void run() {
				JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
				messagePanel.add(
						new JLabel(NbBundle.getMessage(RemoteSshHostProvider.class,
								"MSG_Unresolved_Hosts")), BorderLayout.NORTH); // NOI18N
				JList list = new JList(unresolvedHostsS.toArray());
				list.setVisibleRowCount(4);
				messagePanel.add(new JScrollPane(list), BorderLayout.CENTER);
				NotifyDescriptor dd = new NotifyDescriptor(
						messagePanel,
						NbBundle.getMessage(RemoteSshHostProvider.class,
								"Title_Unresolved_Hosts"), // NOI18N
						NotifyDescriptor.YES_NO_OPTION,
						NotifyDescriptor.ERROR_MESSAGE, null,
						NotifyDescriptor.YES_OPTION);
				if (DialogDisplayer.getDefault().notify(dd) == NotifyDescriptor.NO_OPTION)
					for (File file : unresolvedHostsF)
						Utils.delete(file, true);

				unresolvedHostsF.clear();
				unresolvedHostsS.clear();
			}
		}, 1000);
	}

	private void lockHosts() throws InterruptedException {
		hostsLockedSemaphore.acquire();
	}

	private void unlockHosts() {
		DataSource.EVENT_QUEUE.post(new Runnable() {
			public void run() {
				hostsLockedSemaphore.release();
			}
		});
	}

	public void initialize() {
		WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
			public void run() {
				RequestProcessor.getDefault().post(new Runnable() {
					public void run() {
						initPersistedHosts();
						unlockHosts();
					}
				});
			}
		});
	}

	public RemoteSshHostProvider() {
		try {
			lockHosts(); // Immediately lock the hosts, will be released after
							// initialize()
		} catch (InterruptedException ex) {
			LOGGER.throwing(RemoteSshHostProvider.class.getName(), "<init>", ex); // NOI18N
		}
	}
}