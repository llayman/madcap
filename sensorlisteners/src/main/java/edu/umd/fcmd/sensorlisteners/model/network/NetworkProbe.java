package edu.umd.fcmd.sensorlisteners.model.network;

import edu.umd.fcmd.sensorlisteners.model.Probe;

/**
 * Created by MMueller on 12/2/2016.
 */

public class NetworkProbe extends Probe {
    public static final String NOT_CONNECTED = "NOT_CONNECTED";
    public static final String CONNECTED = "CONNECTED";

    private final String NETWORK_TYPE = "Network";
    private String state;
    private String info;

    /**
     * Gets the state.
     * @return state.
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the state.
     * @param state to be set.
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Gets the SecurityLevel.
     *
     * @return the SecurityLevel.
     */
    public String getInfo() {
        return info;
    }

    /**
     * Sets the SecurityLevel.
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * Gets the type of an state e.g. Accelerometer
     *
     * @return the type of state.
     */
    @Override
    public String getType() {
        return NETWORK_TYPE;
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * possible states: connected or not connected.
     * info: security level
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "{\"state\": " + (state != null ? state : "-") +
                ", \"info\": " + (info != null ? info : "-") +
                '}';
    }
}
