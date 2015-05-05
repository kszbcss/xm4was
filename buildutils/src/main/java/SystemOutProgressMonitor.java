

import org.eclipse.core.runtime.IProgressMonitor;

public class SystemOutProgressMonitor implements IProgressMonitor {
    private boolean canceled;
    
    public void beginTask(String name, int totalWork) {
    }

    public void done() {
    }

    public void internalWorked(double work) {
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean value) {
        canceled = value;
    }

    public void setTaskName(String name) {
    }

    public void subTask(String name) {
        if (name.length() > 0) {
            System.out.println("  " + name);
        }
    }

    public void worked(int work) {
    }
}
