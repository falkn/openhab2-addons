/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.solartracer;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link SolarTracerBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Christian Falk - Initial contribution
 */
public class SolarTracerBindingConstants {

    private static final String BINDING_ID = "solartracer";

    // Config constants
    public static final String CONFIG_PORT = "port";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SOLAR_TRACER = new ThingTypeUID(
            BINDING_ID, "mt5");

    // List of all Channel ids
    public static final String CHANNEL_PV_VOLTAGE = "pv_voltage";
    public static final String CHANNEL_CHARGE_CURRENT = "charge_current";
    public static final String CHANNEL_CHARGE_POWER = "charge_power";

    public static final String CHANNEL_BATT_VOLTAGE = "batt_voltage";
    public static final String CHANNEL_BATT_TEMP = "batt_temp";
    public static final String CHANNEL_BATT_FULL = "batt_full";
    public static final String CHANNEL_BATT_CHARGING = "batt_charging";
    public static final String CHANNEL_BATT_OVERLOAD = "batt_overload";
    public static final String CHANNEL_BATT_OVERDISCHARGE = "batt_overdischarge";
    public static final String CHANNEL_BATT_OVERDISCHARGE_VOLTAGE = "batt_overdischarge_voltage";
    public static final String CHANNEL_BATT_FULL_VOLTAGE = "batt_full_voltage";

    public static final String CHANNEL_LOAD_POWER = "load_power";
    public static final String CHANNEL_LOAD_CURRENT = "load_current";
    public static final String CHANNEL_LOAD_ON = "load_on";
    public static final String CHANNEL_LOAD_OVERLOAD = "load_overload";
    public static final String CHANNEL_LOAD_SHORT = "load_short";

}
