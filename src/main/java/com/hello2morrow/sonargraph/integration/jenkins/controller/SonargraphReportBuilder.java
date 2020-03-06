/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2020 hello2morrow GmbH
 * mailto: support AT hello2morrow DOT com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hello2morrow.sonargraph.integration.jenkins.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.controller.IMetaDataController;
import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphUtil;
import com.hello2morrow.sonargraph.integration.jenkins.model.IMetricIdsHistoryProvider;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.ConfigurationFileWriter;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.ConfigurationFileWriter.MandatoryParameter;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricId;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIds;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIdsHistory;
import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuild;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.RelativePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.model.Project;
import hudson.model.TopLevelItem;
import hudson.model.Queue.FlyweightTask;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * This class contains all the functionality of the build step.
 *
 * @author esteban
 * @author andreas
 *
 */
public final class SonargraphReportBuilder extends AbstractSonargraphRecorder implements IReportPathProvider
{
    private static final String ORG_ECLIPSE_OSGI_JAR = "org.eclipse.osgi_*.jar";
    private static final String SONARGRAPH_BUILD_CLIENT_JAR = "com.hello2morrow.sonargraph.build.client_*.jar";
    private static final String DEFAULT_META_DATA_XML = "MetaData.xml";
    private static final String SONARGRAPH_BUILD_MAIN_CLASS = "com.hello2morrow.sonargraph.build.client.SonargraphBuildRunner";

    public static final int MAX_PORT_NUMBER = 65535;

    private static IMetricIdsHistoryProvider s_metricIdsHistory;

    private final String systemDirectory;
    private final String qualityModelFile;
    private final String virtualModel;
    private final String reportPath;
    private final String baselineReportPath;
    private final String reportGeneration;
    private final String chartConfiguration;
    private final List<Metric> metrics;
    private final String metaDataFile;

    private final boolean languageJava;
    private final boolean languageCSharp;
    private final boolean languageCPlusPlus;
    private final boolean languagePython;

    private final String sonargraphBuildJDK;
    private final String sonargraphBuildVersion;
    private final String activationCode;
    private final String licenseFile;
    private final String workspaceProfile;
    private final String snapshotDirectory;
    private final String snapshotFileName;
    private final String logLevel;
    private final String logFile;

    private final boolean splitByModule;
    private final String elementCountToSplitHtmlReport;
    private final String maxElementCountForHtmlDetailsPage;

    /**
     * Constructor. Fields in the config.jelly/global.jelly must match the parameters in this
     * constructor.
     */
    @DataBoundConstructor
    public SonargraphReportBuilder(final List<Metric> metrics, final String metaDataFile, final String systemDirectory, final String qualityModelFile,
            final String virtualModel, final String reportPath, final String baselineReportPath, final String reportGeneration, final String chartConfiguration,
            final String architectureViolationsAction, final String unassignedTypesAction, final String cyclicElementsAction,
            final String thresholdViolationsAction, final String architectureWarningsAction, final String workspaceWarningsAction,
            final String workItemsAction, final String emptyWorkspaceAction, final boolean languageJava, final boolean languageCSharp,
            final boolean languageCPlusPlus, final boolean languagePython, final String sonargraphBuildJDK, final String sonargraphBuildVersion,
            final String activationCode, final String licenseFile, final String workspaceProfile, final String snapshotDirectory,
            final String snapshotFileName, final String logLevel, final String logFile, final String elementCountToSplitHtmlReport,
            final String maxElementCountForHtmlDetailsPage, final boolean splitByModule)
    {
        super(architectureViolationsAction, unassignedTypesAction, cyclicElementsAction, thresholdViolationsAction, architectureWarningsAction,
                workspaceWarningsAction, workItemsAction, emptyWorkspaceAction);

        this.systemDirectory = systemDirectory;
        this.qualityModelFile = qualityModelFile;
        this.virtualModel = virtualModel;
        this.reportPath = reportPath;
        this.baselineReportPath = baselineReportPath;
        this.reportGeneration = reportGeneration;
        this.chartConfiguration = chartConfiguration;
        this.metaDataFile = metaDataFile;
        this.metrics = metrics;
        this.languageJava = languageJava;
        this.languageCSharp = languageCSharp;
        this.languageCPlusPlus = languageCPlusPlus;
        this.languagePython = languagePython;
        this.sonargraphBuildJDK = sonargraphBuildJDK;
        this.sonargraphBuildVersion = sonargraphBuildVersion;
        this.activationCode = activationCode;
        this.licenseFile = licenseFile;
        this.workspaceProfile = workspaceProfile;
        this.snapshotDirectory = snapshotDirectory;
        this.snapshotFileName = snapshotFileName;
        this.logFile = logFile;
        this.logLevel = logLevel;

        this.splitByModule = splitByModule;
        this.elementCountToSplitHtmlReport = elementCountToSplitHtmlReport;
        this.maxElementCountForHtmlDetailsPage = maxElementCountForHtmlDetailsPage;
    }

    /**
     * We override the getProjectAction method to define our custom action
     * that will show the charts for sonargraph's metrics.
     */
    @Override
    public Collection<Action> getProjectActions(final AbstractProject<?, ?> project)
    {
        final Collection<Action> actions = new ArrayList<>();
        if (project instanceof Project || (project instanceof TopLevelItem && !(project instanceof FlyweightTask)))
        {
            final ResultWithOutcome<MetricIds> result = getMetricIds(project);

            if (result.isSuccess())
            {
                final List<String> metricList = new ArrayList<>();
                final MetricIds exportMetaData = result.getOutcome();
                if (isAllCharts())
                {
                    metricList.addAll(exportMetaData.getMetricIds().keySet());
                }
                else if (isJavaCharts())
                {
                    metricList.addAll(exportMetaData.getMetricIds("JavaLanguageProvider").keySet());
                }
                else if (isCplusplusCharts())
                {
                    metricList.addAll(exportMetaData.getMetricIds("CPlusPlusLanguageProvider").keySet());
                }
                else if (isCsharpCharts())
                {
                    metricList.addAll(exportMetaData.getMetricIds("CSharpLanguageProvider").keySet());
                }
                else if (isPythonCharts())
                {
                    metricList.addAll(exportMetaData.getMetricIds("PythonLanguageProvider").keySet());
                }
                else
                {
                    if (metrics != null)
                    {
                        for (final Metric metric : metrics)
                        {
                            metricList.add(metric.getMetricName());
                        }
                    }
                }
                actions.add(new SonargraphChartAction(project, metricList, exportMetaData));
            }
            else
            {
                SonargraphLogger.INSTANCE.log(Level.SEVERE, "Cannot add SonargraphChartAction, no Meta Data found.");
            }
            actions.add(new SonargraphHTMLReportAction(project));
        }
        return actions;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException
    {
        super.logExecutionStart(build, listener, SonargraphReportBuilder.class);
        if (isGeneratedBySonargraphBuild())
        {
            if (!callSonargraphBuild(build, launcher, listener))
            {
                return false;
            }
        }

        final FilePath sonargraphReportDirectory = new FilePath(build.getWorkspace(), getReportDirectory());
        if (super.processSonargraphReport(build, sonargraphReportDirectory, getReportFileName(), listener.getLogger()))
        {
            //only add the actions after the processing has been successful
            addActions(build);
        }

        /*
         * Must return true for jenkins to mark the build as SUCCESS. Only then,
         * it can be downgraded to the result that was set but it can never be
         * upgraded.
         */
        return true;
    }

    private boolean callSonargraphBuild(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
            throws IOException, InterruptedException
    {
        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO, "Calling Sonargraph Build.", null);

        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null)
        {
            return false;
        }

        JDK jdk;
        final String jdkName = getSonargraphBuildJDK();
        if (jdkName == null || jdkName.isEmpty())
        {
            // no JDK  defined for SonargraphBuild, try to get the one Jenkins was started with
            final List<JDK> allJDKs = jenkins.getJDKs();
            if (allJDKs.size() == 0)
            {
                jdk = new JDK("default", System.getProperty("java.home"));
                SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.WARNING,
                        "Must try to use JDK Jenkins is running with for Sonargraph Build.", null);
            }
            else if (allJDKs.size() == 1)
            {
                jdk = allJDKs.get(0);
                SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO,
                        "Using default JDK '" + jdk.getName() + "' for Sonargraph Build.", null);
            }
            else
            {
                jdk = allJDKs.get(0);
                SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.WARNING,
                        "There are multiple JDKs, please configure one of them. Using JDK '" + jdk.getName()
                                + "' (the first one) for Sonargraph Build.",
                        null);
            }
        }
        else
        {
            jdk = jenkins.getJDK(jdkName);
            SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO, "Using configured JDK '" + jdkName + "' for Sonargraph Build.",
                    null);
        }
        if (jdk == null)
        {
            SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.SEVERE, "Unknown JDK '" + jdkName + "' configured for Sonargraph Build.",
                    null);
            return false;
        }
        jdk = jdk.forNode(build.getBuiltOn(), listener);
        final FilePath javaDir = new FilePath(launcher.getChannel(), jdk.getHome());
        final FilePath javaBinDir = new FilePath(javaDir, "bin");
        final FilePath javaExe = new FilePath(javaBinDir, (File.separatorChar == '\\') ? "java.exe" : "java");

        SonargraphBuild sonargraphBuild;
        final SonargraphBuild.DescriptorImpl descriptor = jenkins.getDescriptorByType(SonargraphBuild.DescriptorImpl.class);

        final String version = getSonargraphBuildVersion();
        if (version == null || version.isEmpty())
        {
            final SonargraphBuild[] allSonargraphBuildInstallations = descriptor.getInstallations();
            if (allSonargraphBuildInstallations.length != 1)
            {
                SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.SEVERE, "Sonargraph Build not configured.", null);
                return false;
            }
            else
            {
                sonargraphBuild = allSonargraphBuildInstallations[0];
            }
        }
        else
        {
            sonargraphBuild = descriptor.getSonargraphBuild(version);
        }
        if (sonargraphBuild == null)
        {
            SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.SEVERE, "Unknown Sonargraph Build configured.", null);
            return false;
        }
        sonargraphBuild = sonargraphBuild.forNode(build.getBuiltOn(), listener);

        final FilePath installationDirectory = new FilePath(launcher.getChannel(), sonargraphBuild.getHome());
        final FilePath pluginsDirectory = new FilePath(installationDirectory, "plugins");
        final FilePath clientDirectory = new FilePath(installationDirectory, "client");

        final FilePath[] osgiJars = pluginsDirectory.list(ORG_ECLIPSE_OSGI_JAR);
        final FilePath osgiJar = osgiJars.length == 1 ? osgiJars[0] : null;
        // pre 8.9.0
        FilePath[] clientJars = pluginsDirectory.list(SONARGRAPH_BUILD_CLIENT_JAR);
        FilePath clientJar = clientJars.length == 1 ? clientJars[0] : null;
        if (clientJar == null)
        {
            // since 8.9.0
            clientJars = clientDirectory.list(SONARGRAPH_BUILD_CLIENT_JAR);
            clientJar = clientJars.length == 1 ? clientJars[0] : null;
        }

        if (osgiJar == null || clientJar == null)
        {
            SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.SEVERE, "Missing plugins in Sonargraph Build installation.", null);
            return false;
        }
        
        final VersionNumber clientVersion = SonargraphUtil.getVersionFromJarName(clientJar.getName());
        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO,
                "SonargraphBuild client jar version is " + clientVersion, null);

        // local configuration file, always on master
        final File configurationFileMaster = File.createTempFile("sonargraphBuildConfigurationMaster", ".xml");

        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO,
                "Writing SonargraphBuild temporary configuration file on master to " + configurationFileMaster.getAbsolutePath(), null);
        final ConfigurationFileWriter writer = new ConfigurationFileWriter(configurationFileMaster);
        final EnumMap<MandatoryParameter, String> parameters = new EnumMap<>(MandatoryParameter.class);
        parameters.put(MandatoryParameter.ACTIVATION_CODE, getActivationCode());
        parameters.put(MandatoryParameter.INSTALLATION_DIRECTORY, sonargraphBuild.getHome());
        parameters.put(MandatoryParameter.LANGUAGES, getLanguages(languageJava, languageCPlusPlus, languageCSharp, languagePython));
        parameters.put(MandatoryParameter.SYSTEM_DIRECTORY, getSystemDirectory());
        parameters.put(MandatoryParameter.REPORT_DIRECTORY, getReportDirectory());
        parameters.put(MandatoryParameter.REPORT_FILENAME, getReportFileName());
        parameters.put(MandatoryParameter.REPORT_TYPE, getReportType());
        parameters.put(MandatoryParameter.REPORT_FORMAT, getReportFormat());
        parameters.put(MandatoryParameter.QUALITY_MODEL_FILE, getQualityModelFile());
        parameters.put(MandatoryParameter.VIRTUAL_MODEL, getVirtualModel());
        parameters.put(MandatoryParameter.LICENSE_FILE, getLicenseFile());

        parameters.put(MandatoryParameter.LICENSE_SERVER_HOST, getDescriptor().getLicenseServerHost());
        parameters.put(MandatoryParameter.LICENSE_SERVER_PORT, getDescriptor().getLicenseServerPort());
        parameters.put(MandatoryParameter.WORKSPACE_PROFILE, getWorkspaceProfile());
        parameters.put(MandatoryParameter.SNAPSHOT_DIRECTORY, getSnapshotDirectory());
        parameters.put(MandatoryParameter.SNAPSHOT_FILE_NAME, getSnapshotFileName());
        parameters.put(MandatoryParameter.LOG_LEVEL, getLogLevel());
        parameters.put(MandatoryParameter.LOG_FILE, getLogFile());

        parameters.put(MandatoryParameter.SPLIT_BY_MODULE, isSplitByModule() ? "true" : "false");
        parameters.put(MandatoryParameter.ELEMENT_COUNT_TO_SPLIT_HTML_REPORT, getElementCountToSplitHtmlReport());
        parameters.put(MandatoryParameter.MAX_ELEMENT_COUNT_FOR_HTML_DETEILS_PAGE, getMaxElementCountForHtmlDetailsPage());
        
        // since 9.12.0.xxx
        VersionNumber since = new VersionNumber("9.12");
        if(clientVersion.isNewerThan(since))
        {
            // possible values are "none" (default), "basic", or "detailed". "detailed" will not work on Jenkins.
            parameters.put(MandatoryParameter.PROGRESS_INFO, "basic");
        }
        
        // since 9.13.0.xxx
        since = new VersionNumber("9.13");
        if(clientVersion.isNewerThan(since))
        {
            parameters.put(MandatoryParameter.REPORT_BASELINE, getBaselineReportPath() + ".xml");
        }

        writer.createConfigurationFile(parameters, listener.getLogger());
        final String content = new FilePath(configurationFileMaster).readToString();
        final FilePath configurationFileSlave = javaDir.createTextTempFile("sonargraphBuildConfigurationSlave", ".xml", content, false);



        // separator taken from launcher, to also work on slaves
        final String classpathSeparator = launcher.isUnix() ? ":" : ";";

        final String sonargraphBuildCommand = handleBlanksForConsoleCommand(javaExe.getRemote()) + " -ea -cp "
                + handleBlanksForConsoleCommand(clientJar.getRemote()) + classpathSeparator + handleBlanksForConsoleCommand(osgiJar.getRemote()) + " "
                + SONARGRAPH_BUILD_MAIN_CLASS + " " + configurationFileSlave.getRemote();

        ProcStarter procStarter = launcher.new ProcStarter();
        procStarter.cmdAsSingleString(sonargraphBuildCommand);
        procStarter.stdout(listener.getLogger());
        final FilePath workspace = build.getWorkspace();
        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO, "Setting working directory for Sonargraph Build to " + workspace, null);
        procStarter = procStarter.pwd(workspace);
        final Proc proc = launcher.launch(procStarter);
        final int processExitCode = proc.join();

        if (processExitCode != 0)
        {
            SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.SEVERE,
                    "There was an error when executing Sonargraph Build. Check the global configuration"
                            + " parameters and the relative paths to make sure that everything is in place.",
                    null);
            return false;
        }
        return true;
    }

    private String handleBlanksForConsoleCommand(final String partOfCommand)
    {
        if (partOfCommand.contains(" "))
        {
            return "\"" + partOfCommand + "\"";
        }
        else
        {
            return partOfCommand;
        }
    }

    private String getReportType()
    {
        return "standard";
    }

    @Override
    public String getReportPath()
    {
        if (isGeneratedBySonargraphBuild())
        {
            return ConfigParameters.SONARGRAPH_REPORT_TARGET_DIRECTORY.getValue() + ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue();
        }
        return reportPath;
    }
    
    public String getBaselineReportPath()
    {
        return baselineReportPath;
    }

    public String getMetaDataFile()
    {
        return metaDataFile;
    }

    public boolean getLanguageJava()
    {
        return languageJava;
    }

    public boolean getLanguageCSharp()
    {
        return languageCSharp;
    }

    public boolean getLanguageCPlusPlus()
    {
        return languageCPlusPlus;
    }

    public boolean getLanguagePython()
    {
        return languagePython;
    }

    /**
     * Returns comma separated list of languages in 'Sonargraph historical' order.
     * @return
     */
    protected static String getLanguages(final boolean languageJava, final boolean languageCPlusPlus, final boolean languageCSharp,
            final boolean languagePython)
    {
        final boolean allLanguages = !(languageJava || languageCPlusPlus || languageCSharp || languagePython);
        final List<String> languages = new ArrayList<>();
        if (allLanguages || languageJava)
        {
            languages.add("Java");
        }
        if (allLanguages || languageCPlusPlus)
        {
            languages.add("CPlusPlus");
        }
        if (allLanguages || languageCSharp)
        {
            languages.add("CSharp");
        }
        if (allLanguages || languagePython)
        {
            languages.add("Python");
        }
        return languages.stream().collect(Collectors.joining(","));
    }

    public String getSonargraphBuildJDK()
    {
        return sonargraphBuildJDK;
    }

    public String getSonargraphBuildVersion()
    {
        return sonargraphBuildVersion;
    }

    public String getActivationCode()
    {
        return activationCode;
    }

    public String getLicenseFile()
    {
        return licenseFile;
    }

    public String getWorkspaceProfile()
    {
        return workspaceProfile;
    }

    public String getSnapshotDirectory()
    {
        return snapshotDirectory;
    }

    public String getSnapshotFileName()
    {
        return snapshotFileName;
    }

    /**
     * @return the logLevel
     */
    public String getLogLevel()
    {
        return logLevel;
    }

    public String getLogFile()
    {
        return logFile;
    }

    private String getReportFileName()
    {
        if (isGeneratedBySonargraphBuild())
        {
            return ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue();
        }
        return new File(getReportPath()).getName();
    }

    @Override
    public String getReportDirectory()
    {
        if (isGeneratedBySonargraphBuild())
        {
            return ConfigParameters.SONARGRAPH_REPORT_TARGET_DIRECTORY.getValue();
        }
        return new File(getReportPath()).getParent();
    }

    private String getReportFormat()
    {
        return "xml,html";
    }

    public String getSystemDirectory()
    {
        return systemDirectory;
    }

    public String getReportGeneration()
    {
        return reportGeneration;
    }

    public String getChartConfiguration()
    {
        return chartConfiguration;
    }

    public String getQualityModelFile()
    {
        return qualityModelFile;
    }

    public String getVirtualModel()
    {
        return virtualModel;
    }

    public String getMaxElementCountForHtmlDetailsPage()
    {
        return maxElementCountForHtmlDetailsPage;
    }

    public String getElementCountToSplitHtmlReport()
    {
        return elementCountToSplitHtmlReport;
    }

    public boolean isSplitByModule()
    {
        return splitByModule;
    }

    public boolean isGeneratedBySonargraphBuild()
    {
        return !isPreGenerated();
    }

    public boolean isPreGenerated()
    {
        return "preGenerated".equals(getReportGeneration());
    }

    public boolean isAllCharts()
    {
        return "allCharts".equals(getChartConfiguration());
    }

    public boolean isJavaCharts()
    {
        return "javaCharts".equals(getChartConfiguration());
    }

    public boolean isCplusplusCharts()
    {
        return "cplusplusCharts".equals(getChartConfiguration());
    }

    public boolean isCsharpCharts()
    {
        return "csharpCharts".equals(getChartConfiguration());
    }

    public boolean isPythonCharts()
    {
        return "pythonCharts".equals(getChartConfiguration());
    }

    public boolean isSelectedCharts()
    {
        return "selectedCharts".equals(getChartConfiguration());
    }

    public List<Metric> getMetrics()
    {
        return metrics;
    }

    @Override
    public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends AbstractBuildStepDescriptor
    {
        static final List<String> DEFAULT_QUALITY_MODELS = Arrays.asList("Sonargraph:Default.sgqm", "Sonargraph:Java.sgqm", "Sonargraph:CSharp.sgqm",
                "Sonargraph:CPlusPlus.sgqm");

        static final List<String> LOG_LEVELS = Arrays.asList("info", "off", "error", "warn", "debug", "trace", "all");

        private String licenseServerHost;
        private String licenseServerPort;

        public DescriptorImpl()
        {
            super();
            load();
        }

        public String getLicenseServerHost()
        {
            return licenseServerHost;
        }

        public String getLicenseServerPort()
        {
            return licenseServerPort;
        }

        public void setLicenseServerHost(final String licenseServerHost)
        {
            this.licenseServerHost = licenseServerHost;
            SonargraphLogger.INSTANCE.log(Level.INFO, "License Server Host is " + licenseServerHost);
        }

        public void setLicenseServerPort(final String licenseServerPort)
        {
            this.licenseServerPort = licenseServerPort;
            SonargraphLogger.INSTANCE.log(Level.INFO, "License Server Port is " + licenseServerPort);
        }

        @Override
        public boolean configure(final StaplerRequest req, JSONObject json) throws FormException
        {
            json = json.getJSONObject("sonargraph");
            setLicenseServerHost(json.getString("licenseServerHost"));
            setLicenseServerPort(json.getString("licenseServerPort"));
            save();
            return true;
        }

        @Override
        public String getDisplayName()
        {
            return ConfigParameters.REPORT_BUILDER_DISPLAY_NAME.getValue();
        }

        public ListBoxModel doFillSonargraphBuildJDKItems()
        {
            final ListBoxModel items = new ListBoxModel();
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null)
            {
                return items;
            }
            for (final JDK jdk : jenkins.getJDKs())
            {
                items.add(jdk.getName(), jdk.getName());
            }

            return items;
        }

        public ListBoxModel doFillSonargraphBuildVersionItems()
        {

            final ListBoxModel items = new ListBoxModel();
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null)
            {
                return items;
            }
            final SonargraphBuild.DescriptorImpl descriptor = jenkins.getDescriptorByType(SonargraphBuild.DescriptorImpl.class);
            for (final SonargraphBuild sonargraphBuild : descriptor.getInstallations())
            {
                items.add(sonargraphBuild.getName(), sonargraphBuild.getName());
            }
            return items;
        }

        public ListBoxModel doFillMetricCategoryItems(@AncestorInPath
        final AbstractProject<?, ?> project)
        {
            final ListBoxModel items = new ListBoxModel();
            final ResultWithOutcome<MetricIds> result = getMetricIds(project);
            if (result.isSuccess())
            {
                final MetricIds metaData = result.getOutcome();
                metaData.getMetricCategories().stream().sorted().forEachOrdered(category -> items.add(category, category));
            }

            return items;
        }

        public ListBoxModel doFillMetricNameItems(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter String metricCategory, @QueryParameter("metaDataFile")
        @RelativePath("..")
        final String metaDataFile)
        {
            if (metricCategory == null || metricCategory.isEmpty())
            {
                SonargraphLogger.INSTANCE.log(Level.WARNING, "metric category is unset, assume 'Architecture'");
                metricCategory = "Architecture";
            }
            final ListBoxModel items = new ListBoxModel();

            final ResultWithOutcome<MetricIds> result = getMetricIds(project);
            if (result.isSuccess())
            {
                final MetricIds metaData = result.getOutcome();
                for (final MetricId metric : metaData.getMetricIdsForCategory(metricCategory))
                {
                    items.add(metric.getName(), metric.getId());
                }
            }
            return items;
        }

        public ComboBoxModel doFillQualityModelFileItems()
        {
            return new ComboBoxModel(DEFAULT_QUALITY_MODELS);
        }

        public FormValidation doCheckLicenseFile(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter
        final String value)
        {
            return checkAbsoluteFile(value, "license");
        }

        public FormValidation doCheckElementCountToSplitHtmlReport(@QueryParameter
        final String value)
        {
            if (value == null || value.isEmpty())
                return FormValidation.ok();

            return checkSplitIntegerParameter(value);
        }

        public FormValidation doCheckMaxElementCountForHtmlDetailsPage(@QueryParameter
        final String value)
        {
            return checkSplitIntegerParameter(value);
        }

        public FormValidation doCheckLicenseServerPort(@QueryParameter
        final String value)
        {
            if (value == null || value.isEmpty())
            {
                return FormValidation.ok();
            }

            try
            {
                final int parsed = Integer.parseUnsignedInt(value);
                if (parsed > 0 && parsed <= MAX_PORT_NUMBER)
                {
                    return FormValidation.ok();
                }
            }
            catch (final NumberFormatException nfe)
            {
                // do nothing
            }
            return FormValidation.error("Please enter a valid port number.");
        }

        public FormValidation doCheckLogFile(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter
        final String value) throws IOException
        {
            final FilePath ws = project.getSomeWorkspace();
            if (ws == null)
            {
                return FormValidation.error("Please run build at least once to get a workspace.");
            }
            final FormValidation validateRelativePath = ws.validateRelativePath(value, false, true);
            if (validateRelativePath.kind != FormValidation.Kind.OK)
            {
                return validateRelativePath;
            }

            final FilePath logfile = new FilePath(ws, value);
            final String logfileURL = project.getAbsoluteUrl() + "ws/" + value;
            return FormValidation.okWithMarkup("Logfile is <a href='" + logfileURL + "'>" + logfile.getRemote() + "</a>");
        }

        public FormValidation doCheckQualityModelFile(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter
        final String value) throws IOException
        {
            if (DEFAULT_QUALITY_MODELS.contains(value))
            {
                return FormValidation.ok();
            }
            return checkFileInWorkspace(project, value, "sgqm");
        }

        public FormValidation doCheckReportPath(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter
        final String value) throws IOException
        {
            if (value != null && !value.isEmpty())
            {
                final File withParent = new File(value);
                final File parent = withParent.getParentFile();
                if (parent == null)
                {
                    return FormValidation.error("Please enter a path with at least one directory.");
                }
                if (value.endsWith(".xml"))
                {
                    return FormValidation.error("Please enter a path without extension \".xml\".");
                }
            }
            return checkFileInWorkspace(project, value + ".xml", null);
        }

        public FormValidation doCheckBaselineReportPath(@AncestorInPath
                final AbstractProject<?, ?> project, @QueryParameter
                final String value) throws IOException
        {
            if (value != null && !value.isEmpty())
            {
                if (value.endsWith(".xml"))
                {
                    return FormValidation.error("Please enter a path without extension \".xml\".");
                }
                return checkFileInWorkspace(project, value + ".xml", null);
            }
            return FormValidation.ok();
        }
        
        /**
         * Split integer parameters must be >= -1.
         * 
         * @param value the value to check.
         * @return FormValidation.ok when value is a valid split integer value, FormValidation.error otherwise.
         */
        private FormValidation checkSplitIntegerParameter(final String value)
        {
            try
            {
                final int parsed = Integer.parseInt(value);
                if (parsed >= -1)
                {
                    return FormValidation.ok();
                }
            }
            catch (final NumberFormatException nfe)
            {
                // do nothing
            }
            return FormValidation.error("Please enter either '-1' (never split), or '0' (use default), or a positive integer value.");
        }

        private FormValidation checkFileInWorkspace(final AbstractProject<?, ?> project, final String file, final String extension) throws IOException
        {
            if (file != null && !file.isEmpty())
            {
                if (extension != null && !extension.isEmpty() && !file.endsWith(extension))
                {
                    return FormValidation.error("Please enter a valid filename. Extension must be '" + extension + "'.");
                }

                final FilePath ws = project.getSomeWorkspace();
                if (ws == null)
                {
                    return FormValidation.error("Please run build at least once to get a workspace.");
                }
                final FormValidation validateRelativePath = ws.validateRelativePath(file, true, true);
                if (validateRelativePath.kind != FormValidation.Kind.OK)
                {
                    return validateRelativePath;
                }
            }
            return FormValidation.ok();
        }

        private FormValidation checkAbsoluteFile(final String file, final String extension)
        {
            if (file != null && !file.isEmpty())
            {
                if (extension != null && !extension.isEmpty() && !file.endsWith(extension))
                {
                    return FormValidation.error("Please enter a valid filename. Extension must be '" + extension + "'.");
                }

                final File f = new File(file);
                if (!f.exists())
                {
                    return FormValidation.error("Please enter an existing file.");
                }
                else if (!f.canRead())
                {
                    return FormValidation.error("Please enter a readable file.");
                }
                else if (!f.isAbsolute())
                {
                    return FormValidation.error("Please enter an absolute file path.");
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckSystemDirectory(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter
        final String value) throws IOException
        {
            if ((value == null) || (value.length() == 0))
            {
                return FormValidation.ok();
            }

            if (!validateNotNullAndRegexp(value, "([a-zA-Z]:\\\\)?[\\/\\\\a-zA-Z0-9_.-]+.sonargraph$"))
            {
                return FormValidation.error("Please enter a valid system directory");
            }
            final FilePath ws = project.getSomeWorkspace();
            if (ws == null)
            {
                return FormValidation.error("Please run build at least once to get a workspace.");
            }
            return ws.validateRelativeDirectory(value, true);
        }

        public ListBoxModel doFillLogLevelItems()
        {
            final ListBoxModel items = new ListBoxModel();
            LOG_LEVELS.forEach(level -> items.add(level));
            return items;
        }

    }

    protected static ResultWithOutcome<MetricIds> getMetricIds(final AbstractProject<?, ?> project)
    {
        final ResultWithOutcome<MetricIds> overallResult = new ResultWithOutcome<>("Get stored MetricIds");

        // get metricIds from history
        if (s_metricIdsHistory == null)
        {
            final File metricIdsHistoryFile = new File(project.getRootDir(), ConfigParameters.METRICIDS_HISTORY_JSON_FILE_PATH.getValue());
            s_metricIdsHistory = new MetricIdsHistory(metricIdsHistoryFile);
        }
        final ResultWithOutcome<MetricIds> historyResult = s_metricIdsHistory.readMetricIds();
        if (historyResult.isFailure())
        {
            overallResult.addMessagesFrom(historyResult);
            return overallResult;
        }

        // get metricIds from export meta data file
        final IMetaDataController controller = ControllerFactory.createMetaDataController();
        final InputStream is = SonargraphReportBuilder.class.getResourceAsStream(DEFAULT_META_DATA_XML);
        final ResultWithOutcome<IExportMetaData> exportMetaDataResult = controller.loadExportMetaData(is, DEFAULT_META_DATA_XML);

        if (exportMetaDataResult.isFailure())
        {
            overallResult.addMessagesFrom(exportMetaDataResult);
            return overallResult;
        }

        // combine and return them
        final MetricIds defaultMetricIds = MetricIds.fromExportMetaData(exportMetaDataResult.getOutcome());
        final MetricIds historyMetricIds = historyResult.getOutcome();
        overallResult.setOutcome(defaultMetricIds.addAll(historyMetricIds));

        return overallResult;
    }

    public static boolean validateNotNullAndRegexp(final String value, final String pattern)
    {
        if (value == null)
        {
            return false;
        }

        if (!value.matches(pattern))
        {
            return false;
        }

        return true;
    }
}
