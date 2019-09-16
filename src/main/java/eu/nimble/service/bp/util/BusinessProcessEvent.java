package eu.nimble.service.bp.util;

public enum BusinessProcessEvent {

    BUSINESS_PROCESS_START("businessProcessStart"), BUSINESS_PROCESS_UPDATE("businessProcessUpdate"), BUSINESS_PROCESS_CANCEL("businessProcessCancel"),
    BUSINESS_PROCESS_COMPLETE("businessProcessComplete");

    private String activity;

    BusinessProcessEvent(String activity){
        this.activity = activity;
    }

    public String getActivity(){
        return activity;
    }
}
