/*******************************************************************************
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2016 hello2morrow GmbH
 * mailto: info AT hello2morrow DOT com
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
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *******************************************************************************/
package com.hello2morrow.sonargraph.integration.jenkins.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.controller.IMetaDataController;
import com.hello2morrow.sonargraph.integration.access.foundation.FileUtility;
import com.hello2morrow.sonargraph.integration.access.foundation.OperationResultWithOutcome;
import com.hello2morrow.sonargraph.integration.access.foundation.StringUtility;
import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.access.model.IMetricCategory;
import com.hello2morrow.sonargraph.integration.access.model.IMetricId;
import com.hello2morrow.sonargraph.integration.access.model.IMetricLevel;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.ConfigurationFileWriter;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.ConfigurationFileWriter.MandatoryParameter;
import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuild;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.RelativePath;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.model.Project;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * This class contains all the functionality of the build step.
 *
 * @author esteban
 * @author andreas
 *
 */
public final class SonargraphReportBuilder extends AbstractSonargraphRecorder implements IReportPathProvider
{
    private static final String DEFAULT_META_DATA_XML = "MetaData.xml";

    private static final String SONARGRAPH_BUILD_MAIN_CLASS = "com.hello2morrow.sonargraph.build.client.SonargraphBuildRunner";

    private final String systemDirectory;
    private final String qualityModelFile;
    private final String virtualModel;
    private final String reportPath;
    private final String reportGeneration;
    private final String chartConfiguration;
    private final List<Metric> metrics;
    private final String metaDataFile;

    private final boolean languageJava;
    private final boolean languageCSharp;
    private final boolean languageCPlusPlus;

    private final String sonargraphBuildJDK;
    private final String sonargraphBuildVersion;
    private final String activationCode;
    private final String licenseFile;
    private final String workspaceProfile;
    private final String snapshotDirectory;
    private final String snapshotFileName;

    /**
     * Constructor. Fields in the config.jelly must match the parameters in this
     * constructor.
     */
    @DataBoundConstructor
    public SonargraphReportBuilder(final List<Metric> metrics, final String metaDataFile, final String systemDirectory, final String qualityModelFile,
            final String virtualModel, final String reportPath, final String reportGeneration, final String chartConfiguration,
            final String architectureViolationsAction, final String unassignedTypesAction, final String cyclicElementsAction,
            final String thresholdViolationsAction, final String architectureWarningsAction, final String workspaceWarningsAction,
            final String workItemsAction, final String emptyWorkspaceAction, final boolean languageJava, final boolean languageCSharp,
            final boolean languageCPlusPlus, final String sonargraphBuildJDK, final String sonargraphBuildVersion, final String activationCode,
            final String licenseFile, final String workspaceProfile, final String snapshotDirectory, final String snapshotFileName)
    {
        super(architectureViolationsAction, unassignedTypesAction, cyclicElementsAction, thresholdViolationsAction, architectureWarningsAction,
                workspaceWarningsAction, workItemsAction, emptyWorkspaceAction);

        this.systemDirectory = systemDirectory;
        this.qualityModelFile = qualityModelFile;
        this.virtualModel = virtualModel;
        this.reportPath = reportPath;
        this.reportGeneration = reportGeneration;
        this.chartConfiguration = chartConfiguration;
        this.metaDataFile = metaDataFile;
        this.metrics = metrics;
        this.languageJava = languageJava;
        this.languageCSharp = languageCSharp;
        this.languageCPlusPlus = languageCPlusPlus;
        this.sonargraphBuildJDK = sonargraphBuildJDK;
        this.sonargraphBuildVersion = sonargraphBuildVersion;
        this.activationCode = activationCode;
        this.licenseFile = licenseFile;
        this.workspaceProfile = workspaceProfile;
        this.snapshotDirectory = snapshotDirectory;
        this.snapshotFileName = snapshotFileName;
    }

    /**
     * We override the getProjectAction method to define our custom action
     * that will show the charts for sonargraph's metrics.
     */
    @Override
    public Collection<Action> getProjectActions(final AbstractProject<?, ?> project)
    {
        final Collection<Action> actions = new ArrayList<>();
        if (project instanceof Project || project instanceof MavenModuleSet)
        {
            try
            {
                final FilePath someWorkspace = project.getSomeWorkspace();
                if (someWorkspace != null)
                {

                    OperationResultWithOutcome<IExportMetaData> result = getMetaData(someWorkspace, getMetaDataFile());

                    if (result.isSuccess())
                    {
                        List<String> metricList;
                        final IExportMetaData exportMetaData = result.getOutcome();
                        if (isAllCharts())
                        {
                            IMetricLevel systemLevel = exportMetaData.getMetricLevels().get("System");
                            List<IMetricId> allSystemMetrics = exportMetaData.getMetricIdsForLevel(systemLevel);
                            metricList = new ArrayList<>(allSystemMetrics.size());
                            for (IMetricId metricId : allSystemMetrics)
                            {
                                metricList.add(metricId.getName());
                            }
                        }
                        else
                        {
                            metricList = new ArrayList<>();
                            if (metrics != null)
                            {
                                for (Metric metric : metrics)
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
                }
                else
                {
                    SonargraphLogger.INSTANCE.log(Level.WARNING, "SonargraphChartAction needs a workspace");
                }
            }
            catch (IOException | InterruptedException e)
            {
                SonargraphLogger.INSTANCE.log(Level.SEVERE, "Cannot add SonargraphChartAction", e);
            }
            actions.add(new SonargraphHTMLReportAction(project, this));
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
        final OperationResultWithOutcome<IExportMetaData> metaData = getMetaData(build.getProject().getSomeWorkspace(), getMetaDataFile());
        if (super.processSonargraphReport(build, sonargraphReportDirectory, getReportFileName(), metaData.getOutcome(), listener.getLogger()))
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
            final List<JDK> allJDKs = jenkins.getJDKs();
            if (allJDKs.size() != 1)
            {
                SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.SEVERE, "Please configure a JDK for Sonargraph Build.", null);
                return false;
            }
            else
            {
                jdk = allJDKs.get(0);
            }
        }
        else
        {
            jdk = jenkins.getJDK(jdkName);
        }
        if (jdk == null)
        {
            SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.SEVERE, "Unknown JDK configured for Sonargraph Build.", null);
            return false;
        }
        jdk = jdk.forNode(build.getBuiltOn(), listener);
        final File javaBinDir = jdk.getBinDir();
        final File javaExe = new File(javaBinDir, (File.separatorChar == '\\') ? "java.exe" : "java");

        SonargraphBuild sonargraphBuild;
        SonargraphBuild.DescriptorImpl descriptor = jenkins.getDescriptorByType(SonargraphBuild.DescriptorImpl.class);

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

        final File configurationFile = File.createTempFile("sonargraphBuildConfiguration", ".xml");
        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO,
                "Writing SonargraphBuild temporary configuration file to " + configurationFile.getAbsolutePath(), null);
        final ConfigurationFileWriter writer = new ConfigurationFileWriter(configurationFile);
        final EnumMap<MandatoryParameter, String> parameters = new EnumMap<>(MandatoryParameter.class);
        parameters.put(MandatoryParameter.ACTIVATION_CODE, getActivationCode());
        parameters.put(MandatoryParameter.INSTALLATION_DIRECTORY, sonargraphBuild.getHome());
        parameters.put(MandatoryParameter.LANGUAGES, getLanguages(languageJava, languageCSharp, languageCPlusPlus));
        parameters.put(MandatoryParameter.SYSTEM_DIRECTORY, getSystemDirectory());
        parameters.put(MandatoryParameter.REPORT_DIRECTORY, getReportDirectory());
        parameters.put(MandatoryParameter.REPORT_FILENAME, getReportFileName());
        parameters.put(MandatoryParameter.REPORT_TYPE, getReportType());
        parameters.put(MandatoryParameter.REPORT_FORMAT, getReportFormat());
        parameters.put(MandatoryParameter.QUALITY_MODEL_FILE, getQualityModelFile());
        parameters.put(MandatoryParameter.VIRTUAL_MODEL, getVirtualModel());
        parameters.put(MandatoryParameter.LICENSE_FILE, getLicenseFile());
        parameters.put(MandatoryParameter.WORKSPACE_PROFILE, getWorkspaceProfile());
        parameters.put(MandatoryParameter.SNAPSHOT_DIRECTORY, getSnapshotDirectory());
        parameters.put(MandatoryParameter.SNAPSHOT_FILE_NAME, getSnapshotFileName());

        writer.createConfigurationFile(parameters, listener.getLogger());

        File installationDirectory = new File(sonargraphBuild.getHome());

        final File pluginsDirectory = new File(installationDirectory, "plugins");
        final File[] osgiJars = FileUtility.listFilesInDirectory(pluginsDirectory, "org.eclipse.osgi_.*\\.jar");
        final File osgiJar = osgiJars.length == 1 ? osgiJars[0] : null;
        final File[] clientJars = FileUtility.listFilesInDirectory(pluginsDirectory, "com.hello2morrow.sonargraph.build.client_.*\\.jar");
        final File clientJar = clientJars.length == 1 ? clientJars[0] : null;

        if (osgiJar == null || clientJar == null)
        {
            SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.SEVERE, "Missing plugins in Sonargraph Build installation.", null);
            return false;
        }

        final String sonargraphBuildCommand = javaExe.getAbsolutePath() + " -ea -cp " + clientJar.getAbsolutePath() + ":" + osgiJar.getAbsolutePath()
                + " " + SONARGRAPH_BUILD_MAIN_CLASS + " " + configurationFile.getAbsolutePath();

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

    protected static String getLanguages(boolean languageJava, boolean languageCSharp, boolean languageCPlusPlus)
    {
        if (languageJava && !languageCSharp && !languageCPlusPlus)
        {
            return "Java";
        }
        else if (!languageJava && languageCSharp && !languageCPlusPlus)
        {
            return "CSharp";
        }
        else if (!languageJava && !languageCSharp && languageCPlusPlus)
        {
            return "CPlusPlus";
        }
        else if (languageJava && languageCSharp && !languageCPlusPlus)
        {
            return "Java,CSharp";
        }
        else if (languageJava && !languageCSharp && languageCPlusPlus) // NOSONAR false positive
        {
            return "Java,CPlusPlus";
        }
        else if (!languageJava && languageCSharp && languageCPlusPlus) // NOSONAR false positive
        {
            return "CSharp,CPlusPlus";
        }
        else
        {
            return "Java,CSharp,CPlusPlus";
        }
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

    public boolean isSelectedCharts()
    {
        return !isAllCharts();
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

        public DescriptorImpl()
        {
            super();
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
            for (JDK jdk : jenkins.getJDKs())
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
            SonargraphBuild.DescriptorImpl descriptor = jenkins.getDescriptorByType(SonargraphBuild.DescriptorImpl.class);
            for (SonargraphBuild sonargraphBuild : descriptor.getInstallations())
            {
                items.add(sonargraphBuild.getName(), sonargraphBuild.getName());
            }
            return items;
        }

        public ListBoxModel doFillMetricCategoryItems(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter("metaDataFile")
        @RelativePath("..")
        final String metaDataFile) throws IOException, InterruptedException
        {
            final ListBoxModel items = new ListBoxModel();
            final FilePath ws = project.getSomeWorkspace();
            final OperationResultWithOutcome<IExportMetaData> result = getMetaData(ws, metaDataFile);
            if (result.isSuccess())
            {
                final IExportMetaData metaData = result.getOutcome();
                List<IMetricId> systemMetrics = metaData.getMetricIdsForLevel(metaData.getMetricLevels().get("System"));
                Set<IMetricCategory> categories = new HashSet<>();

                for (IMetricId metricId : systemMetrics)
                {
                    categories.addAll(metricId.getCategories());
                }

                categories.stream().sorted(new IMetricCategory.MetricCategoryComparator())
                        .forEachOrdered(category -> items.add(category.getPresentationName(), category.getName()));
            }

            return items;
        }

        public ListBoxModel doFillMetricNameItems(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter
        final String metricCategory, @QueryParameter("metaDataFile")
        @RelativePath("..")
        final String metaDataFile) throws IOException, InterruptedException
        {
            final ListBoxModel items = new ListBoxModel();
            final FilePath ws = project.getSomeWorkspace();
            final OperationResultWithOutcome<IExportMetaData> result = getMetaData(ws, metaDataFile);
            if (result.isSuccess())
            {
                final IExportMetaData metaData = result.getOutcome();
                final IMetricCategory category = metaData.getMetricCategories().get(metricCategory);
                if (category != null)
                {
                    for (final IMetricId metric : metaData.getMetricIdsForLevel(metaData.getMetricLevels().get("System")))
                    {
                        if (metric.getCategories().contains(category))
                        {
                            items.add(metric.getPresentationName(), metric.getName());
                        }
                    }
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
        final String value) throws IOException, InterruptedException
        {
            return checkAbsoluteFile(value, "license");
        }

        public FormValidation doCheckQualityModelFile(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter
        final String value) throws IOException, InterruptedException
        {
            if (DEFAULT_QUALITY_MODELS.contains(value))
            {
                return FormValidation.ok();
            }
            return checkFileInWorkspace(project, value, "sgqm");
        }

        public FormValidation doCheckMetaDataFile(@AncestorInPath
        final AbstractProject<?, ?> project, @QueryParameter
        final String value) throws IOException, InterruptedException
        {
            FormValidation validation = checkFileInWorkspace(project, value, "xml");
            if (validation.kind != FormValidation.Kind.OK)
            {
                return validation;
            }
            final OperationResultWithOutcome<IExportMetaData> result = getMetaData(project.getSomeWorkspace(), value);
            if (!result.isSuccess())
            {
                return FormValidation.error(result.toString());
            }
            return FormValidation.ok();
        }

        private FormValidation checkFileInWorkspace(final AbstractProject<?, ?> project, final String file, final String extension)
                throws IOException, InterruptedException
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
                FormValidation validateRelativePath = ws.validateRelativePath(file, true, true);
                if (validateRelativePath.kind != FormValidation.Kind.OK)
                {
                    return validateRelativePath;
                }
            }
            return FormValidation.ok();
        }

        private FormValidation checkAbsoluteFile(final String file, final String extension) throws IOException, InterruptedException
        {
            if (file != null && !file.isEmpty())
            {
                if (extension != null && !extension.isEmpty() && !file.endsWith(extension))
                {
                    return FormValidation.error("Please enter a valid filename. Extension must be '" + extension + "'.");
                }

                File f = new File(file);
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

            if (!StringUtility.validateNotNullAndRegexp(value, "([a-zA-Z]:\\\\)?[\\/\\\\a-zA-Z0-9_.-]+.sonargraph$"))
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

    }

    protected static OperationResultWithOutcome<IExportMetaData> getDefaultMetaData() throws IOException, InterruptedException
    {
        final IMetaDataController controller = new ControllerFactory().createMetaDataController();
        InputStream is = SonargraphReportBuilder.class.getResourceAsStream(DEFAULT_META_DATA_XML);
        return controller.loadExportMetaData(is, DEFAULT_META_DATA_XML);
    }

    protected static OperationResultWithOutcome<IExportMetaData> getMetaData(final FilePath ws, String metaDataFile)
            throws IOException, InterruptedException
    {
        if (ws == null || metaDataFile == null || metaDataFile.isEmpty())
        {
            return getDefaultMetaData();
        }

        FilePath exportMetaDataFile = new FilePath(ws, metaDataFile);
        if (!exportMetaDataFile.exists() || exportMetaDataFile.isDirectory())
        {
            return getDefaultMetaData();
        }

        final IMetaDataController controller = new ControllerFactory().createMetaDataController();
        OperationResultWithOutcome<IExportMetaData> result = controller.loadExportMetaData(exportMetaDataFile.read(),
                exportMetaDataFile.toURI().toString());
        return result;
    }

}
