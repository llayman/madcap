package org.fraunhofer.cese.funf_sensor.backend.models;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Date;

/**
 * Created by llayman on 9/24/2015.
 */
@Entity
public class SensorEntry {

    //attributes
    @Id
    private Long id;                //maybe it will be assigned automatically
    private Date timestamp;
    private String probeType;       //maybe we should use an enum here
    private String sensorData;


    //getters and setters

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date date) {
        this.timestamp = date;
    }

    public String getProbeType() {
        return probeType;
    }

    public void setProbeType(String probeType) {
        this.probeType = probeType;
    }

    public String getSensorData() {
        return sensorData;
    }

    public void setSensorData(String sensorData) {
        this.sensorData = sensorData;
    }

    //Object methods
    @Override
    public String toString() {
        return "Dataset-No.: " + id + "  Captured on: " + (timestamp == null ? "<null>" : timestamp.toString()) +
                "  Type of probe: " + probeType + "  Dataset: " + sensorData;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this)
            return true;

        if (!(obj instanceof SensorEntry))
            return false;

        SensorEntry object = (SensorEntry) obj;

        return this.id == object.getId() && this.timestamp.equals(object.getTimestamp())
                && this.probeType.equals(object.getProbeType())
                && this.sensorData.equals(object.getSensorData());
    }

    @Override
    public int hashCode(){

        int hashCode=17;

        hashCode=31*hashCode+(int)(this.id^(this.id>>>32));
        hashCode=31*hashCode+this.timestamp.hashCode();
        hashCode=31*hashCode+this.probeType.hashCode();
        hashCode=31*hashCode+this.sensorData.hashCode();


        return hashCode;
    }

}
