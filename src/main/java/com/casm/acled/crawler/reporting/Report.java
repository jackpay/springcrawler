package com.casm.acled.crawler.reporting;

import com.casm.acled.camunda.variables.Process;
import com.casm.acled.entities.EntityVersions;
import com.casm.acled.entities.crawlreport.CrawlReport;

import java.time.Instant;

public class Report {
    private final Instant timestamp;
    private final String event;
    private final Integer id;
    // The type of the thing referred to by id
    private final String type;
    // A way of namespacing reports. E.g. set this to ACLEDCommitter.class for all reports generated by it.
    private final String reporterType;
    private final String message;
    private final String businessKey;
    private final String runId;

    public Report(String event, Integer id, String type, String reporterType, String message, String businessKey, String runId, Instant timestamp) {
        this.businessKey = businessKey;
        this.event = event;
        this.id = id;
        this.type = type;
        this.reporterType = reporterType;
        this.message = message;
        this.runId = runId;
        this.timestamp = timestamp;
    }

    public Report message(String format, Object... args) {
        if(args.length == 0) {
            return new Report(event, id, type, reporterType, format, businessKey, runId, timestamp);
        } else {
            return new Report(event, id, type, reporterType, String.format(format, args), businessKey, runId, timestamp);
        }
    }

    public Report append(String format, Object... args) {
        return new Report(event, id, type, reporterType, this.message + " " + String.format(format, args), businessKey, runId, timestamp);
    }

    public Report event(String event){
        return new Report(event, id, type, reporterType, message, businessKey, runId, timestamp);
    }

    public Report event(Event event){
        return event(event.toString());
    }

    public Report id(Integer id) {
        return new Report(event, id, type, reporterType, message, businessKey, runId, timestamp);
    }

    public Report type(Class<?> type) {
        return type(type.getName());
    }
    public Report type(String type){
        return new Report(event, id, type, reporterType, message, businessKey, runId, timestamp);
    }

    public Report reporterType(Class<?> reporterType){
        return reporterType(reporterType.getName());
    }
    public Report reporterType(String reporterType){
        return new Report(event, id, type, reporterType, message, businessKey, runId, timestamp);
    }

    private Report timestamp(Instant timestamp) {
        return new Report(event, id, type, reporterType, message, businessKey, runId, timestamp);
    }
    public Report runId(String runId) {
        return new Report(event, id, type, reporterType, message, businessKey, runId, timestamp);
    }

    public static Report of(Object event, Integer id, String type, String format, Object... args) {
        return new Report(event.toString(), id, type, null, format == null ? null : String.format(format, args), null, null, Instant.now());
    }

    public static Report of(Object event, Integer id, String type) {
        return of(event, id, type, null);
    }

    public static Report of(Object event, Integer id) {
        return of(event, id, null, null);
    }

    public static Report of(Object event) {
        return of(event, null, null, null);
    }

    public static Report of(Integer id, Class<?> type, Class<?> reporterType, String message){
        return new Report(null, id, type.getName(), reporterType.getName(), message, null, null, Instant.now());
    }

    public Instant timestamp() {
        return timestamp;
    };
    public String runId() {
        return runId;
    };
    public String event() {
        return event;
    };
    public Integer id() {
        return id;
    };
    public String type() {
        return type;
    };
    public String reporterType(){
        return reporterType;
    }
    public String message() {
        return message;
    };


    @Override
    public String toString() {
        return "Report{" +
                "timestamp=" + timestamp +
                ", event='" + event + '\'' +
                ", id=" + id +
                ", type='" + type + '\'' +
                ", reporterType='" + reporterType + '\'' +
                ", message='" + message + '\'' +
                ", businessKey='" + businessKey + '\'' +
                ", runId='" + runId + '\'' +
                '}';
    }

    public CrawlReport toCrawlReport() {
        CrawlReport cr = EntityVersions.get(CrawlReport.class).current();
        if(id != null) {
            cr = cr.put(CrawlReport.ID, id);
        }
        if(type != null) {
            cr = cr.put(CrawlReport.TYPE, type);
        }
        if(reporterType != null){
            cr = cr.put(CrawlReport.REPORTER_TYPE, reporterType);
        }
        if(timestamp != null) {
            cr = cr.put(CrawlReport.TIMESTAMP, timestamp);
        }
        if(message != null) {
            cr = cr.put(CrawlReport.MESSAGE, message);
        }
        if(event != null) {
            cr = cr.put(CrawlReport.EVENT, event);
        }
        if(businessKey != null) {
            cr = cr.businessKey(businessKey);
        }
        if(runId != null) {
            cr = cr.put(CrawlReport.RUN_ID, runId);
        }

        return cr;
    }

    public static Report of(CrawlReport cr) {
        Report r = new Report(cr.get(CrawlReport.EVENT),
                cr.get(CrawlReport.ID),
                cr.get(CrawlReport.TYPE),
                cr.get(CrawlReport.REPORTER_TYPE),
                cr.get(CrawlReport.MESSAGE),
                cr.hasBusinessKey() ? cr.businessKey() : "",
                cr.get(CrawlReport.RUN_ID),
                cr.get(CrawlReport.TIMESTAMP));
        return r;
    }
}
