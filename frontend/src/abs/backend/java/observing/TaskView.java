package abs.backend.java.observing;

import java.util.List;

import abs.backend.java.lib.types.ABSValue;

public interface TaskView {
    /**
     * The Task that did the asynchronous call leading to this task
     * is null for the main task, otherwise is never null
     * @return
     */
    TaskView getSender();
    
    /** 
     * The source object of the asynchronous call leading to this task
     * is null for the main task, otherwise is never null
     * @return
     */
    ObjectView getSource();
    
    ObjectView getTarget();
    COGView getCOG();
    String getMethodName();
    List<ABSValue> getArgs();
    FutView getFuture();
    
    void registerTaskListener(TaskObserver listener);

    int getID();
}