/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2023 hello2morrow GmbH
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.model.MetricId;
import com.hello2morrow.sonargraph.integration.jenkins.model.MetricIds;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.ConfigurationFileWriter;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.ConfigurationFileWriter.SonargraphBuildParameter;
import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuild;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.ProxyConfiguration;
import hudson.RelativePath;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.JDK;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * This class contains all the functionality of the build step.
 */
@Symbol("SonargraphReport")
public final class SonargraphReportBuilder extends AbstractSonargraphRecorder
        implements IReportPathProvider, SimpleBuildStep, SimpleBuildStep.LastBuildAction, RunAction2, Serializable
{
    public static final int MAX_PORT_NUMBER = 65535;
    public static final String DO_NOT_SPLIT = "-1";

    public static VersionNumber getVersionFromJarName(String jarName)
    {
        int first = jarName.indexOf("_");
        int second = jarName.indexOf("_", first + 1);
        if (first != -1 && second != -1)
        {
            final String version = jarName.substring(first + 1, second);
            return new VersionNumber(version);
        }
        return new VersionNumber("0.0");
    }

    private static final long serialVersionUID = -4080719672719164118L;
    private static final String ORG_ECLIPSE_OSGI_JAR = "org.eclipse.osgi_*.jar";
    private static final String SONARGRAPH_BUILD_CLIENT_JAR = "com.hello2morrow.sonargraph.build.client_*.jar";
    private static final String SONARGRAPH_BUILD_MAIN_CLASS = "com.hello2morrow.sonargraph.build.client.SonargraphBuildRunner";
    private transient Run<?, ?> run;
    private String systemDirectory = "";
    private String qualityModelFile = "";
    private String virtualModel = DescriptorImpl.DEFAULT_VIRTUAL_MODEL;
    private String reportPath = DescriptorImpl.DEFAULT_REPORT_PATH;
    private String baselineReportPath = "";
    private String reportGeneration = DescriptorImpl.DEFAULT_REPORT_GENERATION;
    private String chartConfiguration = DescriptorImpl.DEFAULT_CHART_CONFIGURATION;
    private List<Metric> metrics;
    private String metaDataFile = "";
    private boolean languageJava;
    private boolean languageCSharp;
    private boolean languageCPlusPlus;
    private boolean languagePython;
    private String sonargraphBuildJDK = "";
    private String sonargraphBuildVersion = "";
    private String activationCode = "";
    private String licenseFile = "";
    private String workspaceProfile = "";
    private String snapshotDirectory = "";
    private String snapshotFileName = "";
    private String logLevel = DescriptorImpl.DEFAULT_LOG_LEVEL;
    private String logFile = DescriptorImpl.DEFAULT_LOG_FILE;
    private boolean skip;
    private boolean useHttpProxy = true;

    @DataBoundConstructor
    public SonargraphReportBuilder()
    {
        super();
    }

    @DataBoundSetter
    public void setSkip(final boolean skip)
    {
        this.skip = skip;
    }

    @DataBoundSetter
    public void setUseHttpProxy(final boolean useHttpProxy)
    {
        this.useHttpProxy = useHttpProxy;
    }

    @DataBoundSetter
    public void setSystemDirectory(final String systemDirectory)
    {
        this.systemDirectory = systemDirectory;
    }

    @DataBoundSetter
    public void setQualityModelFile(final String qualityModelFile)
    {
        this.qualityModelFile = qualityModelFile;
    }

    @DataBoundSetter
    public void setVirtualModel(final String virtualModel)
    {
        this.virtualModel = virtualModel;
    }

    @DataBoundSetter
    public void setReportPath(final String reportPath)
    {
        this.reportPath = reportPath;
    }

    @DataBoundSetter
    public void setBaselineReportPath(final String baselineReportPath)
    {
        this.baselineReportPath = baselineReportPath;
    }

    @DataBoundSetter
    public void setReportGeneration(final String reportGeneration)
    {
        this.reportGeneration = reportGeneration;
    }

    @DataBoundSetter
    public void setChartConfiguration(final String chartConfiguration)
    {
        this.chartConfiguration = chartConfiguration;
    }

    @DataBoundSetter
    public void setMetrics(final List<Metric> metrics)
    {
        this.metrics = metrics;
    }

    @DataBoundSetter
    public void setMetaDataFile(final String metaDataFile)
    {
        this.metaDataFile = metaDataFile;
    }

    @DataBoundSetter
    public void setLanguageJava(final boolean languageJava)
    {
        this.languageJava = languageJava;
    }

    @DataBoundSetter
    public void setLanguageCSharp(final boolean languageCSharp)
    {
        this.languageCSharp = languageCSharp;
    }

    @DataBoundSetter
    public void setLanguageCPlusPlus(final boolean languageCPlusPlus)
    {
        this.languageCPlusPlus = languageCPlusPlus;
    }

    @DataBoundSetter
    public void setLanguagePython(final boolean languagePython)
    {
        this.languagePython = languagePython;
    }

    @DataBoundSetter
    public void setSonargraphBuildJDK(final String sonargraphBuildJDK)
    {
        this.sonargraphBuildJDK = sonargraphBuildJDK;
    }

    @DataBoundSetter
    public void setSonargraphBuildVersion(final String sonargraphBuildVersion)
    {
        this.sonargraphBuildVersion = sonargraphBuildVersion;
    }

    @DataBoundSetter
    public void setActivationCode(final String activationCode)
    {
        this.activationCode = activationCode;
    }

    @DataBoundSetter
    public void setLicenseFile(final String licenseFile)
    {
        this.licenseFile = licenseFile;
    }

    @DataBoundSetter
    public void setWorkspaceProfile(final String workspaceProfile)
    {
        this.workspaceProfile = workspaceProfile;
    }

    @DataBoundSetter
    public void setSnapshotDirectory(final String snapshotDirectory)
    {
        this.snapshotDirectory = snapshotDirectory;
    }

    @DataBoundSetter
    public void setSnapshotFileName(final String snapshotFileName)
    {
        this.snapshotFileName = snapshotFileName;
    }

    @DataBoundSetter
    public void setLogLevel(final String logLevel)
    {
        this.logLevel = logLevel;
    }

    @DataBoundSetter
    public void setLogFile(final String logFile)
    {
        this.logFile = logFile;
    }

    /**
     * We override the getProjectAction method to define our custom action that will show the charts for sonargraph's metrics.
     */
    @Override
    public Collection<Action> getProjectActions(final AbstractProject<?, ?> project)
    {
        final Collection<Action> actions = new ArrayList<>();
        if (JobCategory.isRelevantProject(project))
        {
            final ResultWithOutcome<MetricIds> result = MetricIdProvider.getMetricIds(project);

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
                actions.add(new SonargraphIntegrationAction(project, metricList, exportMetaData));
            }
            else
            {
                SonargraphLogger.INSTANCE.log(Level.SEVERE, "Cannot add SonargraphChartAction, no Meta Data found.");
            }
            actions.add(new InvisibleReportAction(project));
            actions.add(new InvisibleDiffAction(project));
        }
        return actions;
    }

    @Override
    public void perform(final Run<?, ?> run, final FilePath workspace, final Launcher launcher, final TaskListener listener)
            throws InterruptedException, IOException
    {
        super.logExecutionStart(run, listener, SonargraphReportBuilder.class);
        if (isSkip())
        {
            SonargraphLogger.INSTANCE.log(Level.INFO, "Skipping Sonargraph Build");
            return;
        }

        if (isGeneratedBySonargraphBuild())
        {
            if (!callSonargraphBuild(run, workspace, launcher, listener))
            {
                return;
            }
        }

        final FilePath sonargraphReportDirectory = new FilePath(workspace, getReportDirectory());
        if (processSonargraphReport(run, sonargraphReportDirectory, getReportFileName(), listener.getLogger()))
        {
            //only add the actions after the processing has been successful
            run.addAction(new SonargraphBadgeAction());
            run.addAction(new SonargraphReportAction(run));
            if (diffReportCreated())
            {
                run.addAction(new SonargraphDiffAction(run));
            }
        }
    }

    private boolean callSonargraphBuild(final Run<?, ?> build, final FilePath workspace, final Launcher launcher, final TaskListener listener)
            throws IOException, InterruptedException
    {
        assert listener != null : "Parameter 'listener' of method 'callSonargraphBuild' must not be null";
        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO, "Calling Sonargraph Build.", null);

        final Jenkins jenkins = Jenkins.getInstanceOrNull();
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
        jdk = jdk.forNode(workspace.toComputer().getNode(), listener);
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
        sonargraphBuild = sonargraphBuild.forNode(workspace.toComputer().getNode(), listener);

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

        final VersionNumber clientVersion = getVersionFromJarName(clientJar.getName());
        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO, "SonargraphBuild client jar version is " + clientVersion, null);

        // local configuration file, always on master
        final File configurationFileMaster = File.createTempFile("sonargraphBuildConfigurationMaster", ".xml");

        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO,
                "Writing SonargraphBuild temporary configuration file on master to " + configurationFileMaster.getAbsolutePath(), null);
        final ConfigurationFileWriter writer = new ConfigurationFileWriter(configurationFileMaster);
        final EnumMap<SonargraphBuildParameter, String> parameters = new EnumMap<>(SonargraphBuildParameter.class);
        parameters.put(SonargraphBuildParameter.ACTIVATION_CODE, getActivationCode());
        parameters.put(SonargraphBuildParameter.INSTALLATION_DIRECTORY, sonargraphBuild.getHome());
        parameters.put(SonargraphBuildParameter.LANGUAGES, getLanguages(languageJava, languageCPlusPlus, languageCSharp, languagePython));
        parameters.put(SonargraphBuildParameter.SYSTEM_DIRECTORY, getSystemDirectory());
        parameters.put(SonargraphBuildParameter.REPORT_DIRECTORY, getReportDirectory());
        parameters.put(SonargraphBuildParameter.REPORT_FILENAME, getReportFileName());
        parameters.put(SonargraphBuildParameter.REPORT_TYPE, getReportType());
        parameters.put(SonargraphBuildParameter.REPORT_FORMAT, getReportFormat());
        parameters.put(SonargraphBuildParameter.QUALITY_MODEL_FILE, getQualityModelFile());
        parameters.put(SonargraphBuildParameter.VIRTUAL_MODEL, getVirtualModel());
        parameters.put(SonargraphBuildParameter.LICENSE_FILE, getLicenseFile());

        parameters.put(SonargraphBuildParameter.LICENSE_SERVER_HOST, getDescriptor().getLicenseServerHost());
        parameters.put(SonargraphBuildParameter.LICENSE_SERVER_PORT, getDescriptor().getLicenseServerPort());
        parameters.put(SonargraphBuildParameter.WORKSPACE_PROFILE, getWorkspaceProfile());
        parameters.put(SonargraphBuildParameter.SNAPSHOT_DIRECTORY, getSnapshotDirectory());
        parameters.put(SonargraphBuildParameter.SNAPSHOT_FILE_NAME, getSnapshotFileName());
        parameters.put(SonargraphBuildParameter.LOG_LEVEL, getLogLevel());
        parameters.put(SonargraphBuildParameter.LOG_FILE, getLogFile());

        parameters.put(SonargraphBuildParameter.ELEMENT_COUNT_TO_SPLIT_HTML_REPORT, DO_NOT_SPLIT);

        if (isUseHttpProxy())
        {
            ProxyConfiguration proxyConfig = ProxyConfiguration.load();
            if (proxyConfig != null)
            {
                final String host = proxyConfig.getName();
                if (host != null && !host.isEmpty())
                {
                    final int port = proxyConfig.getPort();
                    parameters.put(SonargraphBuildParameter.PROXY_HOST, host);
                    parameters.put(SonargraphBuildParameter.PROXY_PORT, String.valueOf(port));

                    final String username = proxyConfig.getUserName();
                    if (username != null && !username.isEmpty())
                    {
                        parameters.put(SonargraphBuildParameter.PROXY_USERNAME, username);
                        Secret secretPassword = proxyConfig.getSecretPassword();
                        if (secretPassword != null)
                        {
                            final String password = Secret.toString(secretPassword);
                            parameters.put(SonargraphBuildParameter.PROXY_PASSWORD, password);
                        }
                    }
                }
            }
        }

        // since 9.12.0.xxx
        VersionNumber since = new VersionNumber("9.12");
        if (clientVersion.isNewerThan(since))
        {
            // possible values are "none" (default), "basic", or "detailed". "detailed" will not work on Jenkins.
            parameters.put(SonargraphBuildParameter.PROGRESS_INFO, "basic");
        }

        // since 9.13.0.xxx
        since = new VersionNumber("9.13");
        if (clientVersion.isNewerThan(since))
        {
            final String baselineReportPath = getBaselineReportPath();
            if (baselineReportPath != null && !baselineReportPath.isEmpty())
            {
                parameters.put(SonargraphBuildParameter.REPORT_BASELINE, getBaselineReportPath() + ".xml");
            }
        }

        writer.createConfigurationFile(parameters, listener.getLogger());
        final String content = new FilePath(configurationFileMaster).readToString();
        final FilePath configurationFileSlave = javaDir.createTextTempFile("sonargraphBuildConfigurationSlave", ".xml", content, false);

        // separator taken from launcher, to also work on slaves
        final String classpathSeparator = launcher.isUnix() ? ":" : ";";

        final String sonargraphBuildCommand = handleBlanksForConsoleCommand(javaExe.getRemote()) + " -ea -cp "
                + handleBlanksForConsoleCommand(clientJar.getRemote()) + classpathSeparator + handleBlanksForConsoleCommand(osgiJar.getRemote()) + " "
                + SONARGRAPH_BUILD_MAIN_CLASS + " " + configurationFileSlave.getRemote();

        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO, "Call SonargraphBuild with command:\n" + sonargraphBuildCommand, null);
        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO, "Call SonargraphBuild with parameters:\n" + parameters, null);

        ProcStarter procStarter = launcher.new ProcStarter();
        procStarter.cmdAsSingleString(sonargraphBuildCommand);
        procStarter.stdout(listener.getLogger());
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

    public boolean diffReportCreated()
    {
        return getBaselineReportPath() != null && !getBaselineReportPath().isEmpty();
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
     * 
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

    public boolean isSkip()
    {
        return skip;
    }

    public boolean isUseHttpProxy()
    {
        return useHttpProxy;
    }

    @Override
    public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol("SonargraphReport")
    public static final class DescriptorImpl extends AbstractBuildStepDescriptor
    {

        static final List<String> DEFAULT_QUALITY_MODELS = Arrays.asList("Sonargraph:Default.sgqm", "Sonargraph:Java.sgqm", "Sonargraph:CSharp.sgqm",
                "Sonargraph:CPlusPlus.sgqm");

        static final List<String> LOG_LEVELS = Arrays.asList("info", "off", "error", "warn", "debug", "trace", "all");

        public static final String DEFAULT_VIRTUAL_MODEL = "Modifiable.vm";
        public static final String DEFAULT_LOG_FILE = "sonargraph_build.log";
        public static final String DEFAULT_LOG_LEVEL = "info";
        public static final String DEFAULT_REPORT_PATH = "./target/sonargraph/sonargraph-report";
        public static final String DEFAULT_REPORT_GENERATION = "generatedBySonargraphBuild";
        public static final String DEFAULT_CHART_CONFIGURATION = "allCharts";

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
            final Jenkins jenkins = Jenkins.getInstanceOrNull();
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
            final Jenkins jenkins = Jenkins.getInstanceOrNull();
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

        public ListBoxModel doFillMetricCategoryItems(@AncestorInPath final AbstractProject<?, ?> project)
        {
            final ListBoxModel items = new ListBoxModel();
            final ResultWithOutcome<MetricIds> result = MetricIdProvider.getMetricIds(project);
            if (result.isSuccess())
            {
                final MetricIds metaData = result.getOutcome();
                metaData.getMetricCategories().stream().sorted().forEachOrdered(category -> items.add(category, category));
            }

            return items;
        }

        public ListBoxModel doFillMetricNameItems(@AncestorInPath final AbstractProject<?, ?> project, @QueryParameter String metricCategory,
                @QueryParameter("metaDataFile") @RelativePath("..") final String metaDataFile)
        {
            if (metricCategory == null || metricCategory.isEmpty())
            {
                SonargraphLogger.INSTANCE.log(Level.WARNING, "metric category is unset, assume 'Architecture'");
                metricCategory = "Architecture";
            }
            final ListBoxModel items = new ListBoxModel();

            final ResultWithOutcome<MetricIds> result = MetricIdProvider.getMetricIds(project);
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

        public FormValidation doCheckLicenseFile(@AncestorInPath final AbstractProject<?, ?> project, @QueryParameter final String value)
        {
            return checkAbsoluteFile(value, "license");
        }

        public FormValidation doCheckLicenseServerPort(@QueryParameter final String value)
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

        public FormValidation doCheckLogFile(@AncestorInPath final AbstractProject<?, ?> project, @QueryParameter final String value)
                throws IOException
        {
            if (project != null && (value == null || value.isEmpty()))
            {
                return FormValidation.error("Path of log file must be specified.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckQualityModelFile(@AncestorInPath final AbstractProject<?, ?> project, @QueryParameter final String value)
                throws IOException
        {
            if (DEFAULT_QUALITY_MODELS.contains(value))
            {
                return FormValidation.ok();
            }
            return checkFileInWorkspace(project, value, "sgqm");
        }

        public FormValidation doCheckReportPath(@AncestorInPath final AbstractProject<?, ?> project, @QueryParameter final String value)
                throws IOException
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

        public FormValidation doCheckBaselineReportPath(@AncestorInPath final AbstractProject<?, ?> project, @QueryParameter final String value)
                throws IOException
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

        private FormValidation checkFileInWorkspace(final AbstractProject<?, ?> project, final String file, final String extension) throws IOException
        {
            if (project == null)
            {
                return FormValidation.ok();
            }

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

        public FormValidation doCheckSystemDirectory(@AncestorInPath final AbstractProject<?, ?> project, @QueryParameter final String value)
                throws IOException
        {
            if ((value == null) || (value.length() == 0))
            {
                return FormValidation.ok();
            }

            if (!validateNotNullAndRegexp(value, "([a-zA-Z]:\\\\)?[\\/\\\\a-zA-Z0-9_.-]+.sonargraph$"))
            {
                return FormValidation.error("Please enter a valid system directory");
            }
            if (project == null)
            {
                return FormValidation.ok();
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

    @Override
    public String getIconFileName()
    {
        return ConfigParameters.SONARGRAPH_ICON.getValue();
    }

    @Override
    public String getDisplayName()
    {
        return ConfigParameters.ACTION_DISPLAY_INTEGRATION.getValue();
    }

    @Override
    public String getUrlName()
    {
        return ConfigParameters.ACTION_URL_INTEGRATION.getValue();
    }

    @Override
    public Collection<? extends Action> getProjectActions()
    {
        System.out.println("Class of project: " + run.getClass());
        return getProjectActions((AbstractProject<?, ?>) run.getParent());
    }

    @Override
    public void onAttached(Run<?, ?> run)
    {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run)
    {
        this.run = run;
    }

}
