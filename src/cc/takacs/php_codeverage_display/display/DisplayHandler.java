package cc.takacs.php_codeverage_display.display;

import cc.takacs.php_codeverage_display.clover.CloverXmlReader;
import cc.takacs.php_codeverage_display.clover.CoverageCollection;
import cc.takacs.php_codeverage_display.clover.FileCoverage;
import cc.takacs.php_codeverage_display.config.ConfigValues;
import cc.takacs.php_codeverage_display.displaymap.CanonicalDisplayMapDecorator;
import cc.takacs.php_codeverage_display.displaymap.FilenameDisplayMap;
import cc.takacs.php_codeverage_display.displaymap.SimpleFilenameDisplayMap;
import cc.takacs.php_codeverage_display.displaymap.UnixToWindowsDisplayMapDecorator;
import cc.takacs.php_codeverage_display.displaymap.WindowsDisplayMapDecorator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.SystemUtils;

import javax.swing.*;

/**
 * @author Zsolt Takacs <zsolt@takacs.cc>
 */
public class DisplayHandler {
    private FilenameDisplayMap map;
    private ConfigValues configValues;
    private Project project;

    public DisplayHandler(ConfigValues configValues, Project project) {
        this.configValues = configValues;
        this.project = project;

        initializeMap();
    }

    public void initializeMap() {
        createDisplayMap();

        for (VirtualFile file : FileEditorManager.getInstance(project).getOpenFiles()) {
            Editor editor = null;

            for (FileEditor fileEditor : FileEditorManager.getInstance(project).getEditors(file)) {
                if ((fileEditor instanceof TextEditor)) {
                    editor = ((TextEditor) fileEditor).getEditor();
                    break;
                }
            }

            if (editor != null) {
                addDisplayForEditor(editor, file.getPath());
            }
        }
    }

    private void createDisplayMap()
    {
        if (configValues.directoryMapping) {
            this.map = new UnixToWindowsDisplayMapDecorator(new SimpleFilenameDisplayMap(), configValues);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            this.map = new WindowsDisplayMapDecorator(new SimpleFilenameDisplayMap());
        } else {
            this.map = new CanonicalDisplayMapDecorator(new SimpleFilenameDisplayMap());
        }
    }

    public void updateDisplays() {
        CloverXmlReader reader = new CloverXmlReader(getCloverXmlPath());
        CoverageCollection fileCoverages = reader.parse();

        for (String filename : fileCoverages.getKeys()) {
            FileCoverage fileCoverage = fileCoverages.get(filename);

            CoverageDisplay display = map.get(filename);

            if (display != null) {
                display.setFileCoverage(fileCoverage);

                SwingUtilities.invokeLater(new DisplayDrawerThread(display));
            }
        }
    }

    private String getCloverXmlPath() {
        return configValues.getCloverXmlPath();
    }

    public void addDisplayForEditor(Editor editor, String file) {
        CoverageDisplay display = new CoverageDisplay(editor, configValues);

        editor.getDocument().addDocumentListener(display);

        this.map.add(file, display);
    }

    public void removeDisplayForFile(String file) {
        this.map.remove(file);
    }

    public void reassignDisplay(String fromFile, String toFile) {
        CoverageDisplay display = map.get(fromFile);

        if (display != null) {
            map.remove(fromFile);
            map.add(toFile, display);
        }
    }

    public void redrawIfXmlChanged(String path) {
        if (path.equals(configValues.cloverXmlPath)) {
            updateDisplays();
        }
    }
}
